package com.scienaptic.lib.filesources

import com.scienaptic.lib.filesources.inputstream.{ InputStreamConfig, InputStreamConfigCompanion }
import com.scienaptic.lib.filesources.interfaces.InputStreamConfigParamsType
import enumeratum.{ Enum, EnumEntry }

import scala.collection.immutable

sealed trait Storage extends EnumEntry

/** Enum for File Sources */
object Storage extends Enum[Storage] {

  val values: immutable.IndexedSeq[Storage] = findValues

  case object Local extends Storage

  case object HDFS extends Storage

  case object S3 extends Storage

  case object URL extends Storage

  private object DefaultPorts {
    final val HDFS: Int = 9000
  }

  implicit class FileSourceHelper(val storage: Storage) extends AnyVal {

    /** Host for file sources */
    def host: Option[String] = storage match {
      case HDFS => Some("localhost")
      case _    => None
    }

    /** Port for file sources */
    def port: Option[Int] = storage match {
      case HDFS => Some(DefaultPorts.HDFS)
      case _    => None
    }

    /** Get InputStream config case class companion of file source */
    def inputStreamConfigCompanion: Option[InputStreamConfigCompanion] = storage match {
      case Local => Some(inputstream.Local)
      case HDFS  => Some(inputstream.HDFS)
      case S3    => Some(inputstream.S3)
      case URL   => Some(inputstream.URL)
      case _     => None
    }

    /** Get InputStream config case class of file source */
    def inputStreamConfig(params: InputStreamConfigParamsType): Option[InputStreamConfig] =
      storage.inputStreamConfigCompanion match {
        case Some(x) => Some(x(params))
        case _       => None
      }

  }

}
