package com.scienaptic.lib.filesources.mimetype

import java.io.InputStream

import com.scienaptic.lib.filesources.mimetype.MimeType.MimeValue
import org.apache.tika.Tika

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MimeTypeUtils {

  /**
    * Obtain the mime type from an input stream.
    * <strong>Closes the input stream after MIME type detection.</strong>
    *
    * @param inputStream InputStream of the file
    * @return a [[scala.concurrent.Future]] enclosing [[com.scienaptic.lib.filesources.mimetype.MimeType.MimeValue]] of the input
    */
  def detectMimeValueFuture(inputStream: InputStream): Future[MimeValue] = Future {
    val mimeType = new Tika().detect(inputStream)
    inputStream.close()
    mimeType
  }

}
