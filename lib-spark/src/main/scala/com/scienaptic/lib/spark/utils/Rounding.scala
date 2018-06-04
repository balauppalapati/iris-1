package com.scienaptic.lib.spark.utils

object Rounding {

  def toDouble(d: Double, scale: Int): Double =
    BigDecimal(d).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble
}
