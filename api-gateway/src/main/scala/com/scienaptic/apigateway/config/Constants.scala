package com.scienaptic.apigateway.config

object Constants {

  final val ServiceName: String = "api-gateway"

  object Endpoints {

    /**
      * Vert.x circuit breakers publish their metrics in order to be consumed by Hystrix Dashboard
      */
    final val HystrixMetrics: String = "/infra/hystrix-metrics"
  }

}
