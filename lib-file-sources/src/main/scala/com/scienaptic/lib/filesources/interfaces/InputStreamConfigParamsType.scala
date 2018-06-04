package com.scienaptic.lib.filesources.interfaces

import io.vertx.scala.core.http.HttpServerRequest

/**
  * Sum Type Pattern
  * ADT for possible data structure types to create a file source [[java.io.InputStream]].
  * Currently [[io.vertx.scala.core.http.HttpServerRequest]] are listed
  */
sealed trait InputStreamConfigParamsType

final case class InputStreamConfigFromRequest(req: HttpServerRequest) extends InputStreamConfigParamsType
