package com.scienaptic.lib.validation

import inet.ipaddr.HostName

import scala.util.Try

object Validation {

  /**
    * Check given host is valid
    *
    * @param host Hostname to validate
    * @return If host is syntactically correct return scala.util.Success[Unit].
    *         Invalid values have [[inet.ipaddr.HostNameException]] and return scala.util.Failure[Unit]
    *
    */
  def isValidHost(host: String): Try[Unit] =
    Try(new HostName(host).validate())

}
