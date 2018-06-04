package com.scienaptic.lib.common.util

import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import org.scalatest.FunSpec

import scala.util.{ Failure, Success }

class OptionStrictImplicitTest extends FunSpec {

  describe("String to Int conversion") {
    describe("(when valid)") {
      it("should convert valid integer from String to Int") {
        Some("1234").toInt match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed converting valid Int from String representation")
        }
      }
      it("should convert None") {
        None.toInt match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed converting None")
        }
      }
    }
    describe("(when invalid)") {
      it("should complain on invalid Int values") {
        Some("blah").toInt match {
          case Success(_) => fail("Failed: Converts invalid String to Int")
          case Failure(_) => succeed
        }
      }
    }
  }

  describe("String to Double conversion") {
    describe("(when valid)") {
      it("should convert valid number from String to Double") {
        Some("1234.567").toDouble match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed converting valid Double from String representation")
        }
      }
      it("should convert None") {
        None.toDouble match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed converting None")
        }
      }
    }
    describe("(when invalid)") {
      it("should complain on invalid Double values") {
        Some("blah").toDouble match {
          case Success(_) => fail("Failed: Converts invalid String to Double")
          case Failure(_) => succeed
        }
      }
    }
  }

  describe("String to Boolean conversion") {
    describe("(when valid)") {
      it("should convert valid boolean from String to Boolean") {
        Some("false").toBoolean match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed converting valid Boolean from String representation")
        }
      }
      it("should convert None") {
        None.toBoolean match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed converting None")
        }
      }
    }
    describe("(when invalid)") {
      it("should complain on invalid Boolean values") {
        Some("Falsee").toBoolean match {
          case Success(_) => fail("Failed: Converts invalid String to Boolean")
          case Failure(_) => succeed
        }
      }
    }
  }

}
