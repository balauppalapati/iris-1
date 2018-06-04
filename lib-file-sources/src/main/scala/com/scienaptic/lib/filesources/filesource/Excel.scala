package com.scienaptic.lib.filesources.filesource

import com.scienaptic.lib.common.util.Equal._
import java.io.InputStream
import java.util

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions.IllegalFileFormatException
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.filesources.mimetype.MimeType
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel._
import org.apache.poi.xssf.XLSBUnsupportedException
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

object Excel extends Base {

  type ExcelRow  = Seq[String]
  type ExcelRows = Seq[ExcelRow]

  /**
    * Response type from Excel Parsing.
    *
    * @param rows
    */
  final case class ExcelParserResponse(rows: ExcelRows)

  /**
    * Check mime type of file and parse only [[com.scienaptic.lib.filesources.mimetype.MimeType.excelMimeValues]] types.
    * Errors are handled by [[getAttributesErrors]]
    *
    * @param inputStreamFuture InputStream of the file for mimeType Detection.
    * @return
    */
  def getAttributesFuture(inputStreamFuture: => Future[InputStream]): Future[ExcelParserResponse] =
    checkMimeTypeAndParseFile(inputStreamFuture, parseFileFuture(), MimeType.excelMimeValues)

  /**
    * Handles errors caused by getAttributesFuture
    *
    * @param inputStreamErrorHandler Error handler for inputstream.
    * @param pathKey                 Key for the [[com.scienaptic.lib.common.error.Exceptions.IllegalFileFormatException]]
    * @param e                       Exception from getAttributesFuture
    * @return Map of errors with provided key.
    */
  def getAttributesErrors(inputStreamErrorHandler: Throwable => ErrorMap,
                          pathKey: String = "path")(e: Throwable): ErrorMap = e match {
    case InputStreamException(x) => inputStreamErrorHandler(x)
    case x @ (_: IllegalFileFormatException | _: XLSBUnsupportedException) =>
      Map(pathKey -> x.getMessage)
    case NonFatal(_) => Exceptions.unknownException(e)
  }

  /**
    *
    * @param streamWithMime InputStream of the file & MIME type of the file input stream.
    * @return
    */
  private def parseFileFuture()(streamWithMime: InputStreamWithMime): Future[ExcelParserResponse] =
    Future {
      for {
        excelRows <- getExcelRowsFuture(streamWithMime)
      } yield excelRows
    }.flatten

  /**
    * @param streamWithMime InputStream of the file & MIME type of the file input stream.
    * @return
    */
  private def getExcelRowsFuture(streamWithMime: InputStreamWithMime): Future[ExcelParserResponse] = {

    val p = Promise[ExcelParserResponse]

    // TODO: Replace Apache POI User API with Event API to prevent Out of memory errors for large files.
    // http://poi.apache.org/spreadsheet/how-to.html#event_api
    // http://poi.apache.org/spreadsheet/how-to.html#xssf_sax_api
    val result = for {
      workbook <- getWorkbook(streamWithMime)
    } yield {
      val worksheet = workbook.getSheetAt(0)
      val rows      = getRows(workbook, worksheet.rowIterator)
      p.success(ExcelParserResponse(rows))
      workbook.close()
      streamWithMime.inputStream.close()
    }

    result match {
      case Some(_) =>
      case None =>
        p.failure(
          IllegalFileFormatException(invalidFileFormat(streamWithMime.mimeType.entryName, MimeType.excelMimeValues))
        )
    }

    p.future
  }

  /**
    * Get workbook from InputStream.
    *
    * @param streamWithMime InputStream of the file & MIME type of the file input stream.
    * @return
    */
  private def getWorkbook(streamWithMime: InputStreamWithMime): Option[Workbook] =
    streamWithMime.mimeType match {
      case MimeType.Excel        => Some(new XSSFWorkbook(streamWithMime.inputStream))
      case MimeType.ExcelPre2007 => Some(new HSSFWorkbook(streamWithMime.inputStream))
      case _                     => None
    }

  /**
    * Get rows from Excel Workbook
    *
    * @param workbook    Excel Workbook
    * @param rowIterator Row iterator of a Worksheet
    * @return
    */
  private def getRows(workbook: Workbook, rowIterator: util.Iterator[Row]): ExcelRows = {

    def getCellString(cell: Cell) =
      cell.getCellTypeEnum match {
        case CellType.BLANK | CellType.BOOLEAN | CellType.NUMERIC => new DataFormatter().formatCellValue(cell)
        case CellType.STRING                                      => cell.getStringCellValue
        case CellType.FORMULA =>
          val evaluator = workbook.getCreationHelper.createFormulaEvaluator
          new DataFormatter().formatCellValue(cell, evaluator)
        case _ => ""
      }

    @tailrec
    def iterateRows(rows: ExcelRows, rowIterator: util.Iterator[Row], numberOfRecordsToRead: Int): ExcelRows =
      if (rowIterator.hasNext && (numberOfRecordsToRead =/= 0)) {
        val row       = rowIterator.next
        val rowValues = row.cellIterator.asScala.toList.map(getCellString)
        iterateRows(rows :+ rowValues, rowIterator, numberOfRecordsToRead - 1)
      } else {
        rows
      }

    iterateRows(Seq.empty[ExcelRow], rowIterator, NumberOfRecordsToReadDefault)
  }

}
