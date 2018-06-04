package com.scienaptic.common.error

import java.net.HttpURLConnection

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.HTTPStatusCodes
import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.web.RoutingContext

import scala.collection.JavaConverters._

/** ADT for exceptions which can occur when sending a http response. */
sealed trait HttpResponseException extends RuntimeException {
  def message: AnyRef

  def exception: Option[Throwable]

  def statusCode: Int
}

/** Requested route not found. */
final case class PageNotFoundException(message: String,
                                       exception: Option[Throwable] = None,
                                       statusCode: Int = HttpURLConnection.HTTP_NOT_FOUND)
    extends HttpResponseException

object PageNotFoundException {

  def create(ctx: RoutingContext): PageNotFoundException =
    new PageNotFoundException(s"${ctx.request.absoluteURI} not found.")
}

/** Invalid request params or issue caused by request params. */
final case class RequestParamsException(message: JsonObject,
                                        exception: Option[Throwable],
                                        statusCode: Int = HTTPStatusCodes.UnprocessableEntity)
    extends HttpResponseException

object RequestParamsException {

  def create(map: ErrorMap, e: Option[Throwable]): RequestParamsException =
    new RequestParamsException(new JsonObject(map.asJava), e)
}

/** Error on serializing response object to Json. */
final case class JsonEncodeException(message: String = "Error in serialising response",
                                     exception: Option[Throwable] = None,
                                     statusCode: Int = HttpURLConnection.HTTP_INTERNAL_ERROR)
    extends HttpResponseException
