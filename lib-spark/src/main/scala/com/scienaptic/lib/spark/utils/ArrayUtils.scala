package com.scienaptic.lib.spark.utils

object ArrayUtils {

  def intersect(selected: Seq[String], all: Array[String]): Array[String] =
    if (selected.nonEmpty) Array(selected.toArray, all).reduce(_ intersect _) else all

}
