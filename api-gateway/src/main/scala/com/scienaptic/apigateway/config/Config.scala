package com.scienaptic.apigateway.config

object Config {

  object Http {
    final val Host: (String, String) = ("api-gateway.http.host", "0.0.0.0")
    final val Port: (String, Int)    = ("api-gateway.http.port", 9000)
  }

  object Frontend {
    final val Path: (String, String) = ("api-gateway.frontend.path", "web")
  }

}
