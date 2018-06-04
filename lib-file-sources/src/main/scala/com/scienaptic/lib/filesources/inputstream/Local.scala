package com.scienaptic.lib.filesources.inputstream

import java.io.{ FileInputStream, FileNotFoundException }

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import io.vertx.scala.core.http.HttpServerRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

final case class Local(path: String) extends InputStreamConfig {

  /**
    * Get the file input stream for file on local file system.
    * Exception handled by [[getInputStreamErrors]]
    *
    * @return a [[scala.concurrent.Future]] enclosing InputStream
    */
  def getInputStreamFuture: Future[FileInputStream] = Future {
    new FileInputStream(path)
  }

  /**
    * Handle exceptions caused by [[getInputStreamFuture]]
    *
    * @param e Exception from [[getInputStreamFuture]]
    * @return Map of errors with key pointing to the request params.
    */
  def getInputStreamErrors(e: Throwable): ErrorMap = e match {
    // File not found
    case _: FileNotFoundException => Map(Local.K.Path -> s"File not found ($path)")
    // Unknown exception
    case NonFatal(_) => Exceptions.unknownException(e)
  }
}

object Local extends InputStreamConfigCompanion {

  // Keys used in request
  private object K {
    final val Path: String = "path"
  }

  def fromRequest(req: HttpServerRequest): Local = {
    val r    = req.getParam _
    val path = r(K.Path)
    Local(path.get)
  }
}
