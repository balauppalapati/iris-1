package com.scienaptic.datasources

import com.scienaptic.lib.filesources.filesource.Csv.{ CsvParserResponse, CsvRows }
import com.scienaptic.lib.filesources.filesource.Excel.{ ExcelParserResponse, ExcelRows }

object ParseExcel {

  final case class Response(rows: ExcelRows)

  def response(x: ExcelParserResponse): Response =
    Response(
      rows = x.rows
    )
}

object ParseCsv {

  final case class CsvAttributes(delimiter: String, lineSeparator: String, quote: String, escape: String)

  final case class Response(attributes: CsvAttributes, rows: CsvRows)

  def response(x: CsvParserResponse): Response =
    Response(
      attributes = ParseCsv.CsvAttributes(
        delimiter = String.valueOf(x.format.getDelimiter),
        lineSeparator = String.valueOf(x.format.getLineSeparator),
        quote = String.valueOf(x.format.getQuote),
        escape = String.valueOf(x.format.getQuoteEscape)
      ),
      rows = x.rows
    )

}
