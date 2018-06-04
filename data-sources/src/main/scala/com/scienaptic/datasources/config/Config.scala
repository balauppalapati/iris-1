package com.scienaptic.datasources.config

object Config {

  object Http {
    final val Host: (String, String) = ("data-sources.http.host", "0.0.0.0")
    final val Port: (String, Int)    = ("data-sources.http.port", 9001)
  }

}
