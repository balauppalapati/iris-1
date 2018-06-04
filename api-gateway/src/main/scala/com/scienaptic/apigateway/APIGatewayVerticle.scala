package com.scienaptic.apigateway

import java.net.HttpURLConnection

import com.scienaptic.apigateway.config.{ Config, Constants }
import com.scienaptic.common.BaseHttpServerVerticle
import com.scienaptic.common.error.PageNotFoundException
import com.scienaptic.common.util.JsonObjectImplicit.JsonObjectHelper
import com.scienaptic.lib.common.annotations.impure
import com.scienaptic.lib.common.util.ArrayUtils
import com.scienaptic.lib.common.util.Equal._
import io.vertx.core.buffer.Buffer
import io.vertx.scala.circuitbreaker.HystrixMetricHandler
import io.vertx.scala.core.{ Future => VertxFuture }
import io.vertx.scala.ext.web.RoutingContext
import io.vertx.scala.ext.web.client.{ HttpResponse, WebClient }
import io.vertx.scala.ext.web.handler.StaticHandler
import io.vertx.scala.servicediscovery.{ Record, ServiceDiscovery }

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{ Failure, Try }

final class APIGatewayVerticle extends BaseHttpServerVerticle {

  @impure override def start(): Unit = {

    super.start()

    val router = createRouter(vertx)

    // Circuit Breaker SSE to monitor in Hystrix Dashboard
    // TODO: Protect route from external access.
    // TODO: Integrate https://github.com/Netflix/Turbine/wiki
    router.get(Constants.Endpoints.HystrixMetrics).handler(HystrixMetricHandler.create(vertx))

    // api dispatcher
    router.route(s"${APIGatewayVerticle.APIRoutesPrefix}*").handler(apiDispatcher)

    // Keep this at the end. Serves static web assets
    router.route("/*").handler(getStaticHandler)
    // Static handler calls cxt.next() for 404 errors.
    // Catch them with the below handler and send JSON response
    router.route("/*").handler(ctx => ctx.fail(PageNotFoundException.create(ctx)))

    // Setup host and port for this service
    val host = config.getString(Config.Http.Host)
    val port = config.getInteger(Config.Http.Port)

    createAndPublishHttpServerFuture(Constants.ServiceName, router, host, port)
  }

  /**
    * Serves static assets from [[Config.Frontend.Path]] folder.
    */
  @impure private def getStaticHandler: StaticHandler = {
    val path    = config.getString(Config.Frontend.Path)
    val handler = StaticHandler.create(path)
    // Set whether hidden files should be served
    handler.setIncludeHidden(false)
    handler
  }

  /**
    * Dispatch api requests to corresponding micro services.
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def apiDispatcher(ctx: RoutingContext): Unit = circuitBreaker.execute {
    (cbFuture: VertxFuture[Any]) =>
      (for {
        // Get all REST api service records.
        records <- getApiServiceRecordsFuture(discovery)
      } yield
      // Get a service record which can fulfill the current request.
      getCurrentApiServiceRecord(ctx, records) match {
        case Some(record) =>
          // Service found.
          val client = discovery.getReference(record).getAs(classOf[WebClient])
          (for {
            response <- forwardRequestFuture(ctx, client)
          } yield {
            forwardResponse(ctx, response)
            cbFutureOnResponse(cbFuture, response)
            // Release the service object retrieved using `get` methods from the service type
            ServiceDiscovery.releaseServiceObject(discovery, client)
          }).andThen(cbFutureOnException(cbFuture))
        case None =>
          // Not Found
          ctx.fail(PageNotFoundException.create(ctx))
          cbFuture.complete
      }).andThen(cbFutureOnException(cbFuture))

  }

  /**
    * Set success/failure of circuit breaker based on the response.
    *
    * @param cbFuture Circuit Breaker future.
    * @param response Response from the service to which the request was dispatched.
    */
  @impure private def cbFutureOnResponse(cbFuture: VertxFuture[Any], response: HttpResponse[Buffer]) =
    if (response.statusCode >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
      // api endpoint server error, circuit breaker should fail
      cbFuture.fail(s"${response.statusCode} : ${response.body}")
    } else {
      cbFuture.complete
    }

  /**
    * Set failure of circuit breaker because of exception
    *
    * @param cbFuture Circuit Breaker future.
    * @tparam T
    * @return
    */
  @impure def cbFutureOnException[T](cbFuture: VertxFuture[Any]): PartialFunction[Try[T], Unit] = {
    case Failure(e) =>
      logger.error(e)
      cbFuture.fail(e)
  }

  /**
    * Lookups for a set of records registered with type [[BaseHttpServerVerticle.HttpEndpointRestApi]]
    */
  private def getApiServiceRecordsFuture(discovery: ServiceDiscovery): Future[mutable.Buffer[Record]] =
    discovery.getRecordsFuture(_.getType === BaseHttpServerVerticle.HttpEndpointRestApi)

  /**
    * Get a service record which can fulfill the api request.
    *
    * @param ctx     Context for the handling of a request
    * @param records Rest API Service records
    * @return
    */
  private def getCurrentApiServiceRecord(ctx: RoutingContext, records: mutable.Buffer[Record]): Option[Record] = {

    val uri = ctx.request.uri.trim
    if (uri.length <= APIGatewayVerticle.APIRoutesPrefix.length) {
      None
    } else {
      val serviceName = getServiceName(ctx)
      // TODO: Implement better load balancing.
      ArrayUtils.getRandomElement(records.filter(_.getName === serviceName).toArray)
    }
  }

  /**
    * Get the serviceName for the request
    * E.g. /api/data-sources/… => data-sources
    *
    * @param ctx Context for the handling of a request
    * @return
    */
  private def getServiceName(ctx: RoutingContext): String =
    ctx.request.uri.trim.substring(APIGatewayVerticle.APIRoutesPrefix.length).split("/")(0)

  /**
    *
    * @param ctx    Context for the handling of a request
    * @param client WebClient object to access the api service
    * @return
    */
  private def forwardRequestFuture(ctx: RoutingContext, client: WebClient): Future[HttpResponse[Buffer]] = {
    val toReq = client.request(ctx.request.method, getRequestUri(ctx))
    // set headers
    toReq.headers.setAll(ctx.request.headers)
    toReq.sendFuture
  }

  /**
    * Get the new path for the request
    * E.g. /api/data-sources/… => /data-sources/…
    *
    * @param ctx Context for the handling of a request
    * @return
    */
  private def getRequestUri(ctx: RoutingContext): String =
    ctx.request.uri.trim.substring(APIGatewayVerticle.APIRoutesPrefix.length - 1)

  /**
    *
    * @param ctx      Context for the handling of a request
    * @param response Response from the web client.
    */
  private def forwardResponse(ctx: RoutingContext, response: HttpResponse[Buffer]): Unit = {
    // Get headers, status code & body from web client response and transfer to ctx.response
    val toRes = ctx.response
    toRes.setStatusCode(response.statusCode)
    toRes.headers.setAll(response.headers)
    response.body match {
      case Some(body) => toRes.end(body)
      case None       => toRes.end
    }
  }

}

object APIGatewayVerticle {
  // Start and end with "/"
  final val APIRoutesPrefix: String = "/api/"
}
