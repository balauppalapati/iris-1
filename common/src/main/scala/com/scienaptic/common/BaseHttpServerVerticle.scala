package com.scienaptic.common

import java.net.{ BindException, HttpURLConnection, SocketException }

import com.scienaptic.common.error.{ HttpResponseException, JsonEncodeException, RequestParamsException }
import com.scienaptic.lib.common.annotations.impure
import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.HTTPStatusCodes
import com.scienaptic.lib.common.util.Equal._
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{ HttpServer, HttpServerOptions }
import io.vertx.scala.ext.web.handler.{ BodyHandler, CorsHandler, LoggerHandler, ResponseTimeHandler }
import io.vertx.scala.ext.web.{ Router, RoutingContext }
import io.vertx.scala.servicediscovery.Record
import io.vertx.servicediscovery.types.HttpEndpoint

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

class BaseHttpServerVerticle extends BaseMicroServiceVerticle {

  /**
    * Create http server for the REST service.
    *
    * @param router router instance
    * @param host   http host
    * @param port   http port
    * @return Future which contains the actual instance of the webserver that just got started
    */
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  @impure private def createHttpServerFuture(name: String,
                                             router: Router,
                                             host: String,
                                             port: Int): Future[HttpServer] = {

    val serverOptions = HttpServerOptions()
    //    serverOptions.setCompressionSupported(true)

    vertx
      .createHttpServer(serverOptions)
      .requestHandler(router.accept _)
      .listenFuture(port, host)
      .recover {
        /*
         * BindException occurs when the port is used by a different service.
         * SocketException occurs when the port isn't available because of privileges.
         */
        case e @ (_: BindException | _: SocketException) =>
          val errorMsg = s"$name: Unable to create http server. ${e.getMessage}"
          logger.error(errorMsg, e)
          throw new IllegalStateException(errorMsg)
        case NonFatal(e) => {
          logger.error(e)
          throw new RuntimeException(e)
        }
      }
  }

  /**
    * Create http server for the REST service and publish it to service discovery.
    *
    * @param name   service name
    * @param router router instance
    * @param host   http host
    * @param port   http port
    */
  @impure protected final def createAndPublishHttpServerFuture(
      name: String,
      router: Router,
      host: String,
      port: Int,
      endPointType: String = HttpEndpoint.TYPE
  ): Future[(HttpServer, Record)] =
    /*
     * createHttpServerFuture and publishHttpEndpointFuture are executed in parallel.
     * If either one fails, the service is terminated by throwing exception.
     */
    (for {
      httpServer <- createHttpServerFuture(name, router, host, port)
      record     <- publishHttpEndpointFuture(name, host, port, endPointType)
    } yield (httpServer, record)).andThen {
      case Success(_) =>
        endPointType match {
          case BaseHttpServerVerticle.HttpEndpointRestApi =>
            logger.info(s"$name: REST API service is running on port $port on host $host")
          case HttpEndpoint.TYPE => logger.info(s"$name: Service is running on port $port on host $host")
        }
    }

  /**
    * Create a router with Body handler and Error handler
    *
    * @param vertx the deployed Vert.x instance
    * @return Router
    */
  protected final def createRouter(vertx: Vertx): Router = {

    val router = Router.router(vertx)

    // http://vertx.io/docs/vertx-web/scala/#_cors_handling
    val allowedOriginPattern = "*"
    val allowedHeaders       = mutable.Set("Access-Control-Allow-Origin", "Content-Type")
    val corsHandler = CorsHandler
      .create(allowedOriginPattern)
      .allowedHeaders(allowedHeaders)
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.OPTIONS)

    // http://vertx.io/docs/vertx-web/scala/#_request_body_handling
    // http://vertx.io/docs/vertx-web/scala/#_error_handling
    router.route
      .handler(BodyHandler.create)
      .handler(corsHandler)
      .handler(ResponseTimeHandler.create)
      .handler(LoggerHandler.create)
      .failureHandler(failureHandler)
    router
  }

  /**
    * Successful json response
    * <strong>
    * Passing a json string directly to this function misses any errors which can occur
    * during serialisation.
    * </strong>
    *
    * @param ctx         Context for the handling of a request
    * @param jsonEncoder Function which encodes the data to json String.
    */
  @impure protected final def ok(ctx: RoutingContext, jsonEncoder: => String): Unit =
    Try(jsonEncoder) match {
      case Success(json) =>
        ctx.response
          .putHeader("content-type", "application/json")
          .end(json)
      case Failure(e) => ctx.fail(JsonEncodeException(exception = Some(e)))
    }

  /**
    * Gives a partial function which can be used for side effects of handling exceptions from a [[scala.concurrent.Future]]
    *
    * @param ctx Context for the handling of a request
    * @param f   Function which takes an exception and responds with error messages
    * @return Partial function for side effects
    */
  def errors[T](ctx: RoutingContext, f: Throwable => ErrorMap): PartialFunction[Try[T], Unit] = {
    case Failure(e) => fail(ctx, f(e), Some(e))
  }

  /**
    * Failed json response
    *
    * @param ctx Context for the handling of a request
    * @param map
    */
  @impure protected final def fail(ctx: RoutingContext, map: ErrorMap, e: Option[Throwable] = None): Unit =
    ctx.fail(RequestParamsException.create(map, e))

  /**
    * Error Handling for Rest APIs.
    *
    * @note Make sure no exceptions are thrown from this method.
    * @param ctx Context for the handling of a request
    */
  @impure private def failureHandler(ctx: RoutingContext): Unit = {

    val json = new JsonObject()

    val (message, statusCode) = ctx.failure match {

      // Response exceptions
      case x: HttpResponseException =>
        x.exception.foreach { t =>
          logger.error(t)
        }
        (x.message, x.statusCode)

      // Unknown exceptions
      case _ =>
        logger.error(ctx.failure)
        val message =
          HTTPStatusCodes.ErrorMessage.getOrElse(ctx.statusCode, s"Unhandled Error. Status Code: ${ctx.statusCode}")
        // When the status code has not been set yet (it is undefined) its value will be -1.
        val statusCode = if (ctx.statusCode === -1) HttpURLConnection.HTTP_INTERNAL_ERROR else ctx.statusCode
        (message, statusCode)
    }

    json.put("message", message).put("code", statusCode)

    ctx.response
      .setStatusCode(statusCode)
      .putHeader("content-type", "application/json")
      .end(json.encode)
  }
}

object BaseHttpServerVerticle {

  /**
    * Custom HttpEndpoint type
    */
  final val HttpEndpointRestApi: String = "rest-api"
}
