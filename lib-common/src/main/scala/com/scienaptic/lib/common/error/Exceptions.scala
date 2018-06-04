package com.scienaptic.lib.common.error

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap

object Exceptions {

  final val Form_Error = "form"

  def getCause(e: Throwable): Option[Throwable] = Option(e.getCause)

  def unknownException(e: Throwable): ErrorMap = Map("unknown" -> e.getMessage)

  // Custom exceptions for error handling.

  final case class IllegalFileFormatException(message: String) extends Exception(message)

  final case class NeverExecutableCodeException(message: String = "Never executable code exception")
      extends Exception(message)

}
