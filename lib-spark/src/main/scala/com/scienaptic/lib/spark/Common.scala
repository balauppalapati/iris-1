package com.scienaptic.lib.spark

import mist.api.data._
import mist.api.encoding.Encoder

object Common {

  case class CsvAttributes(delimiter: String, lineSeparator: String, quote: String, escape: String)

  object CorrelationMatrix {

    case class Request(filePath: String,
                       attributes: CsvAttributes,
                       method: String = "pearson",
                       columns: Seq[String] = Nil,
                       precision: Option[Int] = None)

    case class Response(headers: Array[String], values: Array[Array[Double]])

    object Response {
      implicit val encoder: Encoder[Response] = new Encoder[Response] {

        def apply(x: Response): JsLikeData =
          JsLikeMap(
            "headers" -> JsLikeData.fromScala(x.headers),
            "values"  -> JsLikeData.fromScala(x.values)
          )
      }
    }

  }

  object VarianceInflationFactor {

    case class Request(filePath: String,
                       attributes: CsvAttributes,
                       columns: Seq[String] = Nil,
                       precision: Option[Int] = None)

    case class Response(headers: Array[String], values: Array[Double])

    object Response {
      implicit val encoder: Encoder[Response] = new Encoder[Response] {

        def apply(x: Response): JsLikeData =
          JsLikeMap(
            "headers" -> JsLikeData.fromScala(x.headers),
            "values"  -> JsLikeData.fromScala(x.values)
          )
      }
    }

  }

}
