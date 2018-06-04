package com.scienaptic.lib.filesources.filesource

import java.io.InputStream

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions.IllegalFileFormatException
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.filesources.mimetype.MimeType
import com.univocity.parsers.common.TextParsingException
import com.univocity.parsers.csv.{ CsvFormat, CsvParser, CsvParserSettings }

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

object Csv extends Base {

  // Default number of columns supported in Csv.
  private final val MaxColumnsDefault: Int = 512
  // Maximum number of columns supported in Csv.
  private final val MaxColumns: Int = 10000

  // Default number of characters in a column supported in Csv.
  private final val MaxCharsPerColumnDefault: Int = 4096
  // Maximum number of characters in a column supported in Csv.
  private final val MaxCharsPerColumn: Int = 100000

  type CsvRows = Seq[Array[String]]

  // Response type from CSV Parsing.
  final case class CsvParserResponse(format: CsvFormat, rows: CsvRows)

  /**
    * Settings to parse Csv.
    *
    * @param maxColumns        Max number of columns to allocate for Csv parsing
    * @param maxCharsPerColumn Max number of characters allowed per column
    * @return CSV parser settings
    */
  def getCsvParseSettings(maxColumns: Option[Int] = None,
                          maxCharsPerColumn: Option[Int] = None,
                          numberOfRecordsToRead: Option[Int] = None): CsvParserSettings = {
    /*
     * Restrict the maximum column count to MaxColumns and maximum characters per column to prevent memory overflow
     * Defines a hard limit of how many columns a record can have (defaults to MaxColumnsDefault).
     * Defines a hard limit of how many characters a column can have (defaults to MaxCharsPerColumnDefault).
     * You need this to avoid OutOfMemory errors in case of inputs that might be inconsistent with the format you
     * are dealing with or user providing a very large number.
     */
    val maxColumnsLimit = maxColumns match {
      case Some(x) => if (x > MaxColumns) MaxColumns else x
      case None    => MaxColumnsDefault
    }
    val maxCharsPerColumnLimit = maxCharsPerColumn match {
      case Some(x) => if (x > MaxCharsPerColumn) MaxCharsPerColumn else x
      case None    => MaxCharsPerColumnDefault
    }
    /*
     * Limit maximum sample rows to NumberOfRecordsToRead
     */
    val numberOfRecordsToReadLimit = numberOfRecordsToRead match {
      case Some(x) => if (x > NumberOfRecordsToRead) NumberOfRecordsToRead else x
      case None    => NumberOfRecordsToReadDefault
    }

    val parserSettings = new CsvParserSettings
    // how many columns a record can have
    parserSettings.setMaxColumns(maxColumnsLimit)
    // maximum number of characters allowed for any given column
    parserSettings.setMaxCharsPerColumn(maxCharsPerColumnLimit)
    // Limit the number of valid records to be parsed before the process is stopped
    parserSettings.setNumberOfRecordsToRead(numberOfRecordsToReadLimit)
    // enable detection of delimiter, line separator and text qualifier.
    parserSettings.detectFormatAutomatically()
    // Empty string for empty("") cells & null(missing) cells in CSV.
    parserSettings.setEmptyValue("")
    parserSettings.setNullValue("")

    parserSettings
  }

  /**
    * Check mime type of file and parse only [[com.scienaptic.lib.filesources.mimetype.MimeType.csvMimeValues]] types.
    * Errors are handled by [[getAttributesErrors]]
    *
    * @param inputStreamFuture InputStream of the file
    * @param parserSettings    Settings to parse Csv
    * @return
    */
  def getAttributesFuture(inputStreamFuture: => Future[InputStream],
                          parserSettings: CsvParserSettings): Future[CsvParserResponse] =
    checkMimeTypeAndParseFile(inputStreamFuture, parseFileFuture(parserSettings), MimeType.csvMimeValues)

  /**
    * Handles errors caused by getAttributesFuture
    *
    * @param inputStreamErrorHandler Error handler for inputstream.
    * @param pathKey                 Key for the [[com.scienaptic.lib.common.error.Exceptions.IllegalFileFormatException]]
    * @param maxColumnsKey           Key for [[com.univocity.parsers.common.TextParsingException]] maxColumns
    * @param maxCharsPerColumnKey    Key for [[com.univocity.parsers.common.TextParsingException]] maxCharsPerColumn
    * @param e                       Exception from getAttributesFuture
    * @return Map of errors with provided key.
    */
  def getAttributesErrors(inputStreamErrorHandler: Throwable => ErrorMap,
                          pathKey: String = "path",
                          maxColumnsKey: String = "maxColumns",
                          maxCharsPerColumnKey: String = "maxCharsPerColumn")(e: Throwable): ErrorMap = e match {
    case InputStreamException(x)       => inputStreamErrorHandler(x)
    case x: IllegalFileFormatException => Map(pathKey -> x.getMessage)
    case x: TextParsingException =>
      val exMessage = x.getMessage
      val error = if (exMessage.contains("setMaxColumns")) {
        (maxColumnsKey,
         exMessage.split("\n")(0) +
         ". Set maxColumns greater than or equal to number of columns in your file")
      } else if (exMessage.contains("setMaxCharsPerColumn")) {
        (maxCharsPerColumnKey,
         exMessage.split("\n")(0) +
         ". Set maxCharsPerColumn greater than or equal to maximum characters present in a column in your file")
      } else {
        ("settings", exMessage)
      }
      Map(error._1 -> error._2)
    case NonFatal(_) => Exceptions.unknownException(e)
  }

  /**
    * Get Csv/Tsv file format attributes and sample rows
    * <strong>Closes the input stream after parsing.</strong>
    *
    * @param settings       Configuration for CSV parsing
    * @param streamWithMime InputStream of the file & MIME type of the file input stream.
    * @return a [[scala.concurrent.Future]] enclosing detected Csv format with sample rows
    */
  private def parseFileFuture(
      settings: CsvParserSettings
  )(streamWithMime: InputStreamWithMime): Future[CsvParserResponse] =
    Future {

      // Begin parsing and read first non empty line from file.
      val parser = new CsvParser(settings)

      // Read all rows limited by NumberOfRecordsToReadDefault.
      // The input stream will be closed automatically
      val rows = parser.parseAll(streamWithMime.inputStream)

      // Obtain the attributes from row parsing.
      val format = parser.getDetectedFormat

      CsvParserResponse(format, rows.asScala)
    }

}
