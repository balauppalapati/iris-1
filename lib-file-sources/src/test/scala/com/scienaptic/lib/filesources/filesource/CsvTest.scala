package com.scienaptic.lib.filesources.filesource

import com.scienaptic.lib.filesources.filesource.Csv.CsvParserResponse
import org.scalatest.{ Assertion, FunSpec }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

class CsvTest extends FunSpec {

  private final val MaxColumns                   = 10000
  private final val MaxCharsPerColumn            = 100000
  private final val NumberOfRecordsToReadDefault = 10
  private final val NumberOfRecordsToRead        = 100

  describe("Csv ParserSettings") {

    it("should not exceed maxColumns") {
      val parserSettings = Csv.getCsvParseSettings(maxColumns = Some(MaxColumns + 1))
      if (parserSettings.getMaxColumns > MaxColumns) {
        fail("Failed: maxColumns exceeded hard limit")
      } else {
        succeed
      }
    }

    it("should not exceed maxCharsPerColumn") {
      val parserSettings = Csv.getCsvParseSettings(maxCharsPerColumn = Some(MaxCharsPerColumn + 1))
      if (parserSettings.getMaxCharsPerColumn > MaxCharsPerColumn) {
        fail("Failed: maxCharsPerColumn exceeded hard limit")
      } else {
        succeed
      }
    }

    it("should not exceed numberOfRecordsToRead") {
      val parserSettings = Csv.getCsvParseSettings(maxCharsPerColumn = Some(NumberOfRecordsToRead + 1))
      if (parserSettings.getNumberOfRecordsToRead > NumberOfRecordsToRead) {
        fail("Failed: numberOfRecordsToRead exceeded max limit")
      } else {
        succeed
      }
    }

    it("should enable delimiter detection") {
      val parserSettings = Csv.getCsvParseSettings()
      if (!parserSettings.isDelimiterDetectionEnabled) {
        fail("Failed: Delimiter detection not enabled")
      } else {
        succeed
      }
    }

    it("should enable quote detection") {
      val parserSettings = Csv.getCsvParseSettings()
      if (!parserSettings.isQuoteDetectionEnabled) {
        fail("Failed: Quote detection not enabled")
      } else {
        succeed
      }
    }

    it("should enable line separator detection") {
      val parserSettings = Csv.getCsvParseSettings()
      if (!parserSettings.isLineSeparatorDetectionEnabled) {
        fail("Failed: Line separator detection not enabled")
      } else {
        succeed
      }
    }

    it("should have empty value as blank string") {
      val parserSettings = Csv.getCsvParseSettings()
      if (parserSettings.getEmptyValue != "") {
        fail("Failed: Empty value is not blank string")
      } else {
        succeed
      }
    }

    it("should have null value as blank string") {
      val parserSettings = Csv.getCsvParseSettings()
      if (parserSettings.getNullValue != "") {
        fail("Failed: Null value is not blank string")
      } else {
        succeed
      }
    }

  }

  private def getAttributesFuture(resourceName: String): Future[Csv.CsvParserResponse] =
    Csv.getAttributesFuture(Future {
      getClass.getClassLoader.getResourceAsStream(resourceName)
    }, Csv.getCsvParseSettings())

  private def parsedResults(x: CsvParserResponse): Assertion =
    if (x.rows.length == NumberOfRecordsToReadDefault) {
      succeed
    } else {
      fail(s"Failed: Parse records doesn't have $NumberOfRecordsToReadDefault rows")
    }

  describe("Csv Attributes") {

    describe("(when valid)") {
      it("should parse valid csv") {
        getAttributesFuture("sample.csv").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Bzip2") {
        getAttributesFuture("sample.csv.bz2").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Gzip") {
        getAttributesFuture("sample.csv.gz").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Xz") {
        getAttributesFuture("sample.csv.xz").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Zip") {
        getAttributesFuture("sample.csv.zip").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Tar") {
        getAttributesFuture("sample.csv.tar").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Gzip compressed CPIO Archive") {
        getAttributesFuture("sample.csv.cpgz").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Bzip2 compressed Tar Archive") {
        getAttributesFuture("sample.csv.tar.bz2").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Gzip compressed Tar Archive") {
        getAttributesFuture("sample.csv.tar.gz").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

      it("should parse valid csv in Xz compressed Tar Archive") {
        getAttributesFuture("sample.csv.tar.xz").onComplete {
          case Success(x) => parsedResults(x)
          case Failure(e) => fail(s"Failed: ${e.getMessage}")
        }
      }

    }

    describe("(when invalid)") {
      it("should complain when file is pdf") {
        getAttributesFuture("sample.pdf").onComplete {
          case Success(_) => fail("Failed: Parsing pdf file as CSV")
          case Failure(_) => succeed
        }
      }

      it("should complain when file is pdf in Bzip2") {
        getAttributesFuture("sample.pdf.bz2").onComplete {
          case Success(_) => fail("Failed: Parsing pdf file in Bzip2 as CSV")
          case Failure(_) => succeed
        }
      }

      it("should complain when file is pdf in Gzip") {
        getAttributesFuture("sample.pdf.gz").onComplete {
          case Success(_) => fail("Failed: Parsing pdf file in Gzip as CSV")
          case Failure(_) => succeed
        }
      }

      it("should complain when file is pdf in Tar") {
        getAttributesFuture("sample.pdf.tar").onComplete {
          case Success(_) => fail("Failed: Parsing pdf file in Tar as CSV")
          case Failure(_) => succeed
        }
      }

      it("should complain when file is pdf in Zip") {
        getAttributesFuture("sample.pdf.zip").onComplete {
          case Success(_) => fail("Failed: Parsing pdf file in Zip as CSV")
          case Failure(_) => succeed
        }
      }

      it("should complain when folder is in Zip") {
        getAttributesFuture("folder.zip").onComplete {
          case Success(_) => fail("Failed: Parsing folder in Zip")
          case Failure(_) => succeed
        }
      }

      it("should complain when folder is in Gzip") {
        getAttributesFuture("folder.tgz").onComplete {
          case Success(_) => fail("Failed: Parsing folder in Gzip")
          case Failure(_) => succeed
        }
      }

      it("should complain when folder is in Tar") {
        getAttributesFuture("folder.tar").onComplete {
          case Success(_) => fail("Failed: Parsing folder in Tar")
          case Failure(_) => succeed
        }
      }

      it("should complain when folder is in Bzip2") {
        getAttributesFuture("folder.tbz2").onComplete {
          case Success(_) => fail("Failed: Parsing folder in Bzip2")
          case Failure(_) => succeed
        }
      }
    }

  }

}
