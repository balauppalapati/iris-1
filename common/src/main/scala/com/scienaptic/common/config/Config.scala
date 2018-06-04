package com.scienaptic.common.config

import io.vertx.circuitbreaker.{ CircuitBreakerOptions => CBOpts }

object Config {

  object CircuitBreaker {
    final val Name: (String, String)       = ("circuit-breaker.name", "circuit-breaker")
    final val MaxFailures: (String, Int)   = ("circuit-breaker.max-failures", CBOpts.DEFAULT_MAX_FAILURES)
    final val Timeout: (String, Long)      = ("circuit-breaker.timeout", CBOpts.DEFAULT_TIMEOUT)
    final val ResetTimeout: (String, Long) = ("circuit-breaker.reset-timeout", CBOpts.DEFAULT_RESET_TIMEOUT)
  }

  object Consul {
    final val Host: (String, String)       = ("consul.host", "localhost")
    final val Port: (String, Int)          = ("consul.port", 8500)
    final val DataCenter: (String, String) = ("consul.data-center", "dc1")
  }

}
