package com.scienaptic.lib.filesources.inputstream

import java.io.InputStream

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.filesources.interfaces.{ InputStreamConfigFromRequest, InputStreamConfigParamsType }
import io.vertx.scala.core.http.HttpServerRequest

import scala.concurrent.Future

/**
  * The Sum Type Polymorphism Pattern
  * [[InputStreamConfig]] is a [[Local]] or [[HDFS]] or ... and
  * it has abstract methods and the concrete implementations are in [[Local]] or [[HDFS]] or ...
  */
trait InputStreamConfig {
  def getInputStreamFuture: Future[InputStream]

  def getInputStreamErrors(e: Throwable): ErrorMap
}

trait InputStreamConfigCompanion {

  def apply(params: InputStreamConfigParamsType): InputStreamConfig = params match {
    case InputStreamConfigFromRequest(req) => fromRequest(req)
  }

  def fromRequest(req: HttpServerRequest): InputStreamConfig

}
