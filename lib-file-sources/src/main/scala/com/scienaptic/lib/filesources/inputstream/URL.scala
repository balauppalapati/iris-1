package com.scienaptic.lib.filesources.inputstream

import java.io.{ FileNotFoundException, InputStream }
import java.net._

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import io.vertx.scala.core.http.HttpServerRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

final case class URL(url: String) extends InputStreamConfig {

  /**
    * Get the file input stream for url
    * Exception handled by [[getInputStreamErrors]]
    *
    * @return a [[scala.concurrent.Future]] enclosing InputStream
    */
  def getInputStreamFuture: Future[InputStream] = Future {

    HttpURLConnection.setFollowRedirects(URL.UrlFollowRedirects)

    val connection = new java.net.URL(url).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(URL.UrlConnectionTimeout)
    connection.setReadTimeout(URL.UrlReadTimeout)
    connection.setRequestMethod(URL.UrlRequestMethod)
    connection.connect()
    connection.getInputStream
  }

  /**
    * Handle exceptions caused by [[getInputStreamFuture]]
    *
    * @param e Exception from [[getInputStreamFuture]]
    * @return Map of errors with key pointing to the request params.
    */
  def getInputStreamErrors(e: Throwable): ErrorMap = e match {
    // Unable to resolve host.
    case _: UnknownHostException => Map(URL.K.Path -> s"Unknown host: $url")
    // No protocol at the start of the url
    case _: MalformedURLException => Map(URL.K.Path -> s"No protocol in url: $url")
    // Connection refused
    case _: ConnectException => Map(URL.K.Path -> s"Connection refused: $url")
    // Valid host and port. Url not found.
    case _: FileNotFoundException => Map(URL.K.Path -> s"Url not found: $url")
    // Read time out
    case _: SocketTimeoutException => Map(URL.K.Path -> s"Read timed out: $url")
    // Unknown exception
    case NonFatal(_) => Exceptions.unknownException(e)
  }
}

object URL extends InputStreamConfigCompanion {

  // Keys used in request
  private object K {
    final val Path: String = "path"
  }

  // Sets whether HTTP redirects (requests with response code 3xx) should be automatically followed by this class.
  private final val UrlFollowRedirects: Boolean = true
  // Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource
  // referenced by this URLConnection. If the timeout expires before the connection can be established,
  // a java.net.SocketTimeoutException is raised.
  private final val UrlConnectionTimeout: Int = 5000
  // Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies the timeout when
  // reading from Input stream when a connection is established to a resource. If the timeout expires before
  // there is data available for read, a java.net.SocketTimeoutException is raised.
  private final val UrlReadTimeout: Int = 5000
  // Method for the URL request
  private final val UrlRequestMethod: String = "GET"

  def fromRequest(req: HttpServerRequest): URL = {
    val r    = req.getParam _
    val path = r(K.Path)
    URL(path.get)
  }

}
