package com.scienaptic.lib.datasources.interfaces

import io.vertx.scala.core.http.HttpServerRequest

/**
  * Sum Type Pattern
  * ADT for possible data structure types to create a DSN config [[com.scienaptic.lib.datasources.jdbc.DsnConfig]].
  * Currently [[io.vertx.scala.core.http.HttpServerRequest]] are listed
  */
sealed trait DsnConfigParamsType

final case class DsnConfigFromRequest(req: HttpServerRequest) extends DsnConfigParamsType
