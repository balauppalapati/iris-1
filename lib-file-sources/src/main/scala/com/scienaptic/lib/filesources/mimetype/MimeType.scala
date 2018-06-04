package com.scienaptic.lib.filesources.mimetype

import java.io.{ BufferedInputStream, InputStream }
import java.util.zip.{ ZipEntry, ZipInputStream }

import com.scienaptic.lib.common.error.Exceptions.{ IllegalFileFormatException, NeverExecutableCodeException }
import com.scienaptic.lib.common.util.Equal._
import com.scienaptic.lib.filesources.mimetype.MimeType.MimeValue
import enumeratum.{ Enum, EnumEntry }
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }

/**
  * Enum for supported MIME types for different file parsing.
  */
sealed abstract class MimeType(override val entryName: MimeValue) extends EnumEntry

object MimeType extends Enum[MimeType] {

  type MimeValue = String

  /**
    * Base class for all archivers. Should not include compressors with archiving capability.
    *
    * @param entryName
    */
  sealed abstract class ArchiverMimeType(override val entryName: MimeValue) extends MimeType(entryName)

  /**
    * Base class for all compressors. Should include compressors with archiving capability.
    *
    * @param entryName
    */
  sealed abstract class CompressorMimeType(override val entryName: MimeValue) extends MimeType(entryName)

  /**
    * Compressors which also archiving capability like [[MimeType.Zip]]
    *
    * @param entryName
    */
  sealed abstract class CompressorWithArchiverMimeType(override val entryName: MimeValue)
      extends CompressorMimeType(entryName)

  /**
    * Compressors which doesn't have archiving capabilities like [[MimeType.Gzip]], [[MimeType.Bzip2]]
    *
    * @param entryName
    */
  sealed abstract class CompressorOnlyMimeType(override val entryName: MimeValue) extends CompressorMimeType(entryName)

  /**
    * List of supported [[MimeType]] for different file parsing.
    */
  val values: immutable.IndexedSeq[MimeType] = findValues

  /*
   * -------------------------------------------------------------------------------------------------------------------
   * Files
   * -------------------------------------------------------------------------------------------------------------------
   */

  /**
    * Delimiter separated formats - CSV, TSV
    */
  case object Csv extends MimeType("text/plain")

  /**
    * Microsoft Excel file format 2007.
    */
  case object ExcelPre2007 extends MimeType("application/vnd.ms-excel")

  /**
    * Microsoft Excel file format 2007 and later.
    */
  case object Excel extends MimeType("application/x-tika-ooxml")

  /*
   * -------------------------------------------------------------------------------------------------------------------
   * Archivers
   * -------------------------------------------------------------------------------------------------------------------
   */

  /**
    * Copy In and Out Archive
    */
  case object Cpio extends ArchiverMimeType("application/x-cpio") // https://en.wikipedia.org/wiki/Cpio

  /**
    * Tape Archive(tarball)
    */
  case object Tar extends ArchiverMimeType("application/x-tar")

  /*
   * -------------------------------------------------------------------------------------------------------------------
   * Compressors
   * -------------------------------------------------------------------------------------------------------------------
   */

  /**
    * Compression program that uses the Burrows–Wheeler algorithm
    */
  case object Bzip2 extends CompressorOnlyMimeType("application/x-bzip2") // https://en.wikipedia.org/wiki/Bzip2

  /**
    * gzip is based on the DEFLATE algorithm, which is a combination of LZ77 and Huffman coding
    */
  case object Gzip extends CompressorOnlyMimeType("application/gzip") // https://en.wikipedia.org/wiki/Gzip

  /**
    * xz is a lossless compression program and file format which incorporates the LZMA/LZMA2 compression algorithms.
    */
  case object Xz extends CompressorOnlyMimeType("application/x-xz") // https://en.wikipedia.org/wiki/Xz

  /**
    * Archive file format that supports lossless data compression
    */
  case object Zip extends CompressorWithArchiverMimeType("application/zip")

  /*
   * -------------------------------------------------------------------------------------------------------------------
   * MIME type groups
   * -------------------------------------------------------------------------------------------------------------------
   */

  /**
    * List of Archivers
    */
  private val archivers: Seq[MimeType] =
    findValues.filter(_ match {
      case _: ArchiverMimeType => true
      case _                   => false
    })

  /**
    * List of Compressors
    */
  private val compressors: Seq[MimeType] =
    findValues.filter(_ match {
      case _: CompressorMimeType => true
      case _                     => false
    })

  /**
    * List of Archivers & Compressors
    */
  private def archiversAndCompressors: Seq[MimeType] = archivers ++ compressors

  /**
    * List of MIME values for Csv files
    */
  def csvMimeValues: Seq[MimeValue] = Seq(Csv).map(_.entryName)

  /**
    * List of MIME values for Excel files
    */
  def excelMimeValues: Seq[MimeValue] = Seq(ExcelPre2007, Excel).map(_.entryName)

  /**
    * List of MIME values for Archivers
    */
  def archiversMimeValues: Seq[MimeValue] = archivers.map(_.entryName)

  /**
    * List of MIME values for Archivers & Compressors
    */
  def archiversAndCompressorsMimeValues: Seq[MimeValue] = archiversAndCompressors.map(_.entryName)

  /*
   * -------------------------------------------------------------------------------------------------------------------
   * Helpers
   * -------------------------------------------------------------------------------------------------------------------
   */

  /**
    * Get the input stream of the compressor
    *
    * @param mimeType    MIME type of the compressor
    * @param inputStream InputStream of the file
    * @return
    */
  private def getCompressorInputStream(mimeType: CompressorOnlyMimeType,
                                       inputStream: InputStream): CompressorInputStream =
    mimeType match {
      case Bzip2 => new BZip2CompressorInputStream(inputStream) // *.bz2, *.tbz2, *.cpbz2
      case Gzip  => new GzipCompressorInputStream(inputStream) // *.gz, *.tgz, *.cpgz
      case Xz    => new XZCompressorInputStream(inputStream) // *.xz, *.txz, *.cpxz
    }

  /**
    * Get the input stream of the archiver
    *
    * @see [[org.apache.commons.compress.archivers.ArchiveStreamFactory#createArchiveInputStream]]
    * @param mimeType    MIME type of the archive.
    * @param inputStream InputStream of the file
    * @return
    */
  private def getArchiverInputStream(mimeType: ArchiverMimeType, inputStream: InputStream): ArchiveInputStream =
    mimeType match {
      case Cpio => new CpioArchiveInputStream(inputStream) // *.cpio
      case Tar  => new TarArchiveInputStream(inputStream) // *.tar
    }

  implicit class MimeTypesHelper(val mimeType: MimeType) extends AnyVal {

    /**
      * Is archive/compressed file
      */
    def isWrappedFile: Boolean = archiversAndCompressors.contains(mimeType)

    /**
      * Get input stream of the first entry(file) inside archive/compressed files.
      *
      * @param inputStream       InputStream of the file
      * @param archiverMimeValue MIME type of the archive.
      * @return
      */
    def streamOfWrappedFileFuture(inputStream: InputStream,
                                  archiverMimeValue: Option[MimeValue] = None): Future[InputStream] = {

      val p = Promise[InputStream]

      val bufferedInputStream = new BufferedInputStream(inputStream)

      mimeType match {

        // Archivers

        case x: ArchiverMimeType =>
          p.completeWith(getArchiverStreamFuture(bufferedInputStream, Some(x.entryName)))

        // Compressors

        case x: CompressorOnlyMimeType =>
          val stream = getCompressorInputStream(x, bufferedInputStream)
          p.completeWith(getArchiverStreamFuture(stream, archiverMimeValue))

        case Zip => // *.zip
          p.completeWith(getZipStreamFuture(bufferedInputStream))

        case _ => p.failure(NeverExecutableCodeException())
      }

      p.future
    }

    /**
      * Get the [[MimeType.Zip]] stream
      *
      * @param inputStream InputStream of the file
      * @return
      */
    private def getZipStreamFuture(inputStream: InputStream): Future[InputStream] = {
      val p      = Promise[InputStream]
      val stream = new ZipInputStream(inputStream)
      val entry  = stream.getNextEntry // get the first entry.
      if (Seq(ZipEntry.STORED, ZipEntry.DEFLATED).contains(entry.getMethod)) {
        p.success(stream)
      } else {
        p.failure(IllegalFileFormatException("Unsupported compression method entry. Zipx files are not supported."))
      }
      p.future
    }

    /**
      * Get the [[ArchiverMimeType]] stream
      *
      * @param inputStream             InputStream of the file
      * @param archiverMimeValueOption MIME type of the archive.
      * @return
      */
    private def getArchiverStreamFuture(inputStream: InputStream,
                                        archiverMimeValueOption: Option[MimeValue]): Future[InputStream] = {

      val inputStreamOption: Option[Future[InputStream]] = for {
        archiverMimeValue <- archiverMimeValueOption
        mimeType          <- withNameInsensitiveOption(archiverMimeValue)
      } yield
        mimeType match {

          case archiverMimeType: ArchiverMimeType =>
            Future {
              val stream    = getArchiverInputStream(archiverMimeType, inputStream)
              val entry     = stream.getNextEntry // get the first entry.
              val entryName = entry.getName
              /*
               * ./._
               * OS X's tar uses the AppleDouble format to store extended attributes and Access Control Lists(ACLs).
               *
               */
              if (entryName.startsWith("./._") || (entryName === "./") || (entryName === ".")) {
                stream.getNextEntry
              }
              stream
            }

          case _ => Future.successful(inputStream)
        }

      inputStreamOption match {
        case Some(x) => x
        case None    => Future.successful(inputStream)
      }
    }

  }

}
