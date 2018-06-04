package com.scienaptic.lib.filesources

import enumeratum.{ Enum, EnumEntry }

import scala.collection.immutable

sealed trait FileSource extends EnumEntry

/** Enum for File Sources */
object FileSource extends Enum[FileSource] {
  val values: immutable.IndexedSeq[FileSource] = findValues

  case object CSV extends FileSource

  case object Excel extends FileSource

}
