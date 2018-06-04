package com.scienaptic.lib.filesources.inputstream

import java.io.FileNotFoundException
import java.net.{ ConnectException, UnknownHostException }

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.filesources.Storage
import io.vertx.scala.core.http.HttpServerRequest
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{ FSDataInputStream, FileSystem, LocalFileSystem, Path }
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.net.ConnectTimeoutException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

final case class HDFS(path: String, host: String, port: Int) extends InputStreamConfig {

  /**
    * Get the file input stream for file on hdfs file system.
    * Config without setting implementation(E.g. `fs.hdfs.impl`)
    * will cause [[org.apache.hadoop.fs.UnsupportedFileSystemException]]
    *
    * Exception handled by [[getInputStreamErrors]]
    *
    * @return a [[scala.concurrent.Future]] enclosing InputStream
    */
  def getInputStreamFuture: Future[FSDataInputStream] = Future {

    // https://hadoop.apache.org/docs/r2.9.0/hadoop-project-dist/hadoop-common/core-default.xml
    val config = new Configuration
    // File system implementations.
    config.set("fs.hdfs.impl", classOf[DistributedFileSystem].getName)
    config.set("fs.file.impl", classOf[LocalFileSystem].getName)
    // nameNode host and port
    config.set("fs.defaultFS", s"hdfs://$host:$port")
    // Timeout when connecting to nameNode.
    config.set("ipc.client.connect.timeout", HDFS.IpcClientConnectTimeout)
    config.set("ipc.client.connect.max.retries.on.timeouts", HDFS.IpcClientConnectMaxRetriesOnTimeouts)

    FileSystem.get(config).open(new Path(path))
  }

  /**
    * Handle exceptions caused by [[getInputStreamFuture]]
    *
    * @param e Exception from [[getInputStreamFuture]]
    * @return Map of errors with key pointing to the request params.
    */
  def getInputStreamErrors(e: Throwable): ErrorMap = e match {

    // File not found
    case _: FileNotFoundException =>
      Map(HDFS.K.Path -> s"File not found ($path)")

    // Connection refused
    case _: ConnectException =>
      Map(
        HDFS.K.Host -> (s"Connection refused to name node ($host:$port). " +
        "Check if the name node service is in stopped state. " +
        "It takes time before name node service becomes fully functional after restart.")
      )

    // Mostly with wrong host(valid host name)
    case _: ConnectTimeoutException =>
      Map(
        HDFS.K.Host ->
        (s"Timeout occurred when connecting to name node. Recheck the host value ($host). " +
        "For more details see: http://wiki.apache.org/hadoop/SocketTimeout")
      )

    // Mostly with valid host and wrong port(valid port range)
    case _: java.io.EOFException =>
      Map(
        HDFS.K.Port ->
        (s"Unable to connect to name node. Recheck the port value ($port). " +
        "For more details see: http://wiki.apache.org/hadoop/EOFException")
      )

    case x: IllegalArgumentException =>
      Exceptions.getCause(x) match {
        // Invalid host name. E.g. localhost1
        case Some(_: UnknownHostException) => Map(HDFS.K.Host -> s"Unknown host mentioned in name node ($host)")
        // Unknown exception
        case Some(NonFatal(_)) => Exceptions.unknownException(e)
        // Invalid port. E.g. 90001
        case None => Map(HDFS.K.Port -> e.getMessage)
      }

    // Unknown exception
    case NonFatal(_) => Exceptions.unknownException(e)
  }
}

object HDFS extends InputStreamConfigCompanion {

  // Keys used in request
  private object K {
    final val Path: String = "path"
    final val Host: String = "host"
    final val Port: String = "port"
  }

  // Indicates the number of milliseconds a client will wait for the socket to establish a server connection.
  private final val IpcClientConnectTimeout: String = "4000"
  // Indicates the number of retries a client will make on socket timeout to establish a server connection.
  private final val IpcClientConnectMaxRetriesOnTimeouts: String = "3"

  def fromRequest(req: HttpServerRequest): HDFS = {

    val r = req.getParam _

    val path = r(K.Path)
    val host = r(K.Host) orElse Storage.HDFS.host
    val port = r(K.Port).toInt.toOption.flatten orElse Storage.HDFS.port

    HDFS(path.get, host.get, port.get)
  }

}
