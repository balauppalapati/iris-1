package com.scienaptic.lib.common.util

import scala.util.{ Failure, Success, Try }

/**
  * Convert String to other types{Int, Boolean..} with exceptions trapped inside [[scala.util.Try]].
  *
  * @note Used to convert http request parameters to the needed type.
  */
object OptionStringImplicit {

  implicit class OptionStringHelper(val param: Option[String]) extends AnyVal {

    /**
      * Convert String to Integer with exception trapped on failure.
      * [[scala.NumberFormatException]] will be enclosed in failure
      *
      */
    def toInt: Try[Option[Int]] = toType(_.toInt)

    /**
      * Convert String to Double with exception trapped on failure.
      * [[scala.NumberFormatException]] will be enclosed in failure
      *
      */
    def toDouble: Try[Option[Double]] = toType(_.toDouble)

    /**
      * Convert String to Integer with exception trapped on failure.
      * [[scala.IllegalArgumentException]] will be enclosed in failure
      *
      */
    def toBoolean: Try[Option[Boolean]] = toType(_.toBoolean)

    /**
      * Convert String to type A with exception trapped on failure.
      *
      * @param f Function which converts String to type A
      * @tparam A Destination type of String conversion.
      */
    private def toType[A](f: String => A): Try[Option[A]] = param match {
      case Some(s) =>
        Try(f(s)) match {
          case Success(value) => Success(Option(value))
          case Failure(e)     => Failure(e)
        }
      case None => Success(None)
    }

  }

}
