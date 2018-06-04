package com.scienaptic.common.util

import io.circe.{ Encoder, Json }

object JsonEncoder {

  val nullAsEmptyString: Encoder[String] = {
    case null => Json.fromString("") // scalastyle:ignore
    case s    => Json.fromString(s)
  }

}
