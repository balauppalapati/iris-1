package com.scienaptic.lib.common.error

import java.net.HttpURLConnection

/**
  * Error messages for various HTTP Status Codesr
  */
object HTTPStatusCodes {

  final val UnprocessableEntity: Int = 422

  // https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
  final val ErrorMessage: Map[Int, String] = Map(
    // HTTP Status-Code 400: Bad Request.
    // The request was unacceptable, often due to missing a required parameter.
    HttpURLConnection.HTTP_BAD_REQUEST -> "Bad Request",
    // HTTP Status-Code 401: Unauthorized.
    // No valid Auth key provided.
    HttpURLConnection.HTTP_UNAUTHORIZED -> "Unauthorized",
    // HTTP Status-Code 403: Forbidden.
    HttpURLConnection.HTTP_FORBIDDEN -> "Forbidden",
    // HTTP Status-Code 404: Not Found.
    // The requested resource doesn't exist.
    HttpURLConnection.HTTP_NOT_FOUND -> "Not Found",
    // HTTP Status-Code 422: Unprocessable Entity
    // The request was well-formed but was unable to be followed due to semantic errors
    UnprocessableEntity -> "Unprocessable Entity",
    // HTTP Status-Code 500: Internal Server Error.
    HttpURLConnection.HTTP_INTERNAL_ERROR -> "Internal Server Error.",
    // HTTP Status-Code 501: Not Implemented.
    HttpURLConnection.HTTP_NOT_IMPLEMENTED -> "Not Implemented.",
    // HTTP Status-Code 502: Bad Gateway.
    HttpURLConnection.HTTP_BAD_GATEWAY -> "Bad Gateway.",
    //  HTTP Status-Code 503: Service Unavailable.
    HttpURLConnection.HTTP_UNAVAILABLE -> "Service Unavailable.",
    // HTTP Status-Code 504: Gateway Timeout.
    // Gateway or Proxy, did not receive a timely response from the upstream server
    HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> "Gateway Timeout.",
    // Undefined Status-Code. Convert it to HTTP_INTERNAL_ERROR
    -1 -> "Unhandled error by application. Contact developer."
  )

}
