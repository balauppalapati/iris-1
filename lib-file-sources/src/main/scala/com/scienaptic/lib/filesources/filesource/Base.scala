package com.scienaptic.lib.filesources.filesource

import java.io.{ BufferedInputStream, InputStream }

import com.scienaptic.lib.common.error.Exceptions.IllegalFileFormatException
import com.scienaptic.lib.common.util.Equal._
import com.scienaptic.lib.filesources.mimetype.MimeType.MimeValue
import com.scienaptic.lib.filesources.mimetype.{ MimeType, MimeTypeUtils }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Try }

private[filesource] class Base {

  // Default number of valid records to be parsed before the process is stopped
  protected final val NumberOfRecordsToReadDefault: Int = 10
  // Maximum number of valid records to be parsed before the process is stopped
  protected final val NumberOfRecordsToRead: Int = 1000

  // InputStream of the file and its MIME type.
  protected case class InputStreamWithMime(inputStream: InputStream, mimeType: MimeType)

  // Exception on getting inputStream of file.
  protected case class InputStreamException(e: Throwable) extends Exception

  protected def invalidFileFormat(mimeValue: MimeValue, supportedMimeValues: Seq[MimeValue]): String =
    s"Invalid file format: $mimeValue. Expected formats: [${supportedMimeValues.mkString(", ")}]"

  /**
    * Check mime type of file and parse only supported types.
    * Errors are handled by getAttributesErrors
    *
    * @param inputStreamFuture InputStream of the file
    * @param parse             function to parse the file and return results.
    * @param fileMimeValues    MIME values of files supported for parsing
    * @tparam T Type of the result returned by parsing the file
    * @return
    */
  protected def checkMimeTypeAndParseFile[T](inputStreamFuture: => Future[InputStream],
                                             parse: InputStreamWithMime => Future[T],
                                             fileMimeValues: Seq[MimeValue]): Future[T] = {

    val p = Promise[T]

    // Errors to retrieve InputStream.
    def inputStreamFutureErrors[A]: PartialFunction[Try[A], p.type] = {
      case Failure(e) => p.failure(InputStreamException(e))
    }

    // Errors from other futures - MIME type detection & parsing.
    def errors[A]: PartialFunction[Try[A], p.type] = {
      case Failure(e) => p.failure(e)
    }

    // Get future result to use in for comprehension and recover to prevent multiple executions
    val mimeValueFuture = for {
      inputStream <- inputStreamFuture andThen inputStreamFutureErrors
      mimeValue   <- MimeTypeUtils.detectMimeValueFuture(new BufferedInputStream(inputStream)) andThen errors
    } yield mimeValue

    // Merge supported file MIME values with Archive & Compressed MIME values
    val mimeValues = fileMimeValues ++ MimeType.archiversAndCompressorsMimeValues

    (for {
      // Get MIME value
      mimeValue <- mimeValueFuture if mimeValues.contains(mimeValue)
      // Get the file InputStream of file with MIME type. Unwraps if the file is archived/compressed.
      streamWithMimeForParse <- getInputStreamWithMimeFuture(inputStreamFuture,
                                                             MimeType.withName(mimeValue),
                                                             fileMimeValues) andThen errors
      results <- parse(streamWithMimeForParse) andThen errors
    } yield p.success(results)).andThen {

      //  Occurs when the predicate isn't satisfied above.
      case Failure(_: NoSuchElementException) =>
        // Detected MIME type is not in supported types.
        mimeValueFuture.foreach(
          mimeValue => p.failure(IllegalFileFormatException(invalidFileFormat(mimeValue, mimeValues)))
        )
    }

    p.future

  }

  /**
    * Get input stream of the file and its MIME type. Unwraps the file from archived/compressed file.
    *
    * @param inputStreamFuture InputStream of the file
    * @param mimeType          MIME type of the file input stream.
    * @param fileMimeValues    MIME values of files supported for parsing
    * @return
    */
  private def getInputStreamWithMimeFuture(inputStreamFuture: => Future[InputStream],
                                           mimeType: MimeType,
                                           fileMimeValues: Seq[MimeValue]): Future[InputStreamWithMime] = {

    val p = Promise[InputStreamWithMime]

    // Errors from other futures
    def errors[A]: PartialFunction[Try[A], p.type] = {
      case Failure(e) => p.failure(e)
    }

    if (mimeType.isWrappedFile) {
      // File is inside archive/compressed file.
      for {
        // Get the MIME value of the file inside archive/compressed file
        mimeValueOfWrappedFile <- getMimeValueOfWrappedFile(inputStreamFuture, mimeType, None) andThen errors
      } yield {
        // Is the file inside the wrap is archive - tar/cpio/...
        if (MimeType.archiversMimeValues.contains(mimeValueOfWrappedFile)) {
          // The file is inside archive. Get the MIME value of the file inside the archive.
          for {
            archiverMimeValue <- getMimeValueOfWrappedFile(inputStreamFuture, mimeType, Some(mimeValueOfWrappedFile)) andThen errors
          } yield
          // Get the stream of the file inside the archive.
          p.completeWith(
            getStreamWithMimeOfWrappedFile(inputStreamFuture, mimeType, fileMimeValues, archiverMimeValue)
          )
        } else {
          // Get the stream of the file inside the wrap.
          p.completeWith(
            getStreamWithMimeOfWrappedFile(inputStreamFuture, mimeType, fileMimeValues, mimeValueOfWrappedFile)
          )
        }
      }
    } else {
      // Uncompressed & unarchived file.
      for {
        inputStream <- inputStreamFuture
      } yield p.success(InputStreamWithMime(new BufferedInputStream(inputStream), mimeType))
    }

    p.future
  }

  /**
    * Get the MIME value of file inside archive/compressed file
    *
    * @param inputStreamFuture InputStream of the file
    * @param mimeType          MIME type of the file input stream.
    * @param archiverMimeValue MIME type of the file inside archive.
    * @return
    */
  private def getMimeValueOfWrappedFile(inputStreamFuture: => Future[InputStream],
                                        mimeType: MimeType,
                                        archiverMimeValue: Option[MimeValue]): Future[MimeValue] =
    for {
      inputStream            <- inputStreamFuture
      streamOfWrappedFile    <- mimeType.streamOfWrappedFileFuture(inputStream, archiverMimeValue)
      mimeValueOfWrappedFile <- MimeTypeUtils.detectMimeValueFuture(streamOfWrappedFile)
    } yield mimeValueOfWrappedFile

  /**
    * Get the stream of the file inside archive/compressed wrap.
    *
    * @param inputStreamFuture InputStream of the file
    * @param mimeType          MIME type of the file input stream.
    * @param fileMimeValues    MIME values of files supported for parsing
    * @param wrappedMimeValue  MIME value of the file inside wrap(archived/compressed)
    * @return
    */
  private def getStreamWithMimeOfWrappedFile(inputStreamFuture: => Future[InputStream],
                                             mimeType: MimeType,
                                             fileMimeValues: Seq[MimeValue],
                                             wrappedMimeValue: MimeValue): Future[InputStreamWithMime] =
    if (fileMimeValues.contains(wrappedMimeValue)) {
      val archiverMimeValue = MimeType.archiversMimeValues.find(_ === wrappedMimeValue)
      for {
        inputStream         <- inputStreamFuture
        streamOfWrappedFile <- mimeType.streamOfWrappedFileFuture(inputStream, archiverMimeValue)
      } yield InputStreamWithMime(streamOfWrappedFile, MimeType.withName(wrappedMimeValue))
    } else {
      Future.failed(
        IllegalFileFormatException(
          s"Invalid file inside archive: $wrappedMimeValue. " +
          s"Archive should have a single file of type: [${fileMimeValues.mkString(", ")}]"
        )
      )
    }

}
