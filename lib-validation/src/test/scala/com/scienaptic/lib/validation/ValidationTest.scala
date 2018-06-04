package com.scienaptic.lib.validation

import org.scalatest.FunSpec

import scala.util.{ Failure, Success }

class ValidationTest extends FunSpec {

  /**
    * Test cases for [[Validation.isValidHost]]
    */
  describe("A Host") {

    // https://en.wikipedia.org/wiki/Hostname#cite_note-2

    describe("(when valid)") {
      it("should parse IPv4 format") {
        Validation.isValidHost("192.168.1.1") match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed valid IPv4 format")
        }
      }
      it("should parse IPv6 format") {
        Validation.isValidHost("2001:470:20::2") match {
          case Success(_) => succeed
          case Failure(_) => fail("Failed valid IPv6 format")
        }
      }
    }

    describe("(when invalid)") {
      // Host can't start with a dash
      it("should complain when host starts with minus") {
        Validation.isValidHost("-localhost") match {
          case Success(_) => fail("Failed validating host which starts with dash")
          case Failure(_) => succeed
        }
      }
      // Host can't end with a dash
      it("should complain when host ends with minus") {
        Validation.isValidHost("localhost-") match {
          case Success(_) => fail("Failed validating host which ends with dash")
          case Failure(_) => succeed
        }
      }
      // Host can't have spaces
      it("should complain when host has a space") {
        Validation.isValidHost("local host") match {
          case Success(_) => fail("Failed validating host which has space")
          case Failure(_) => succeed
        }
      }
      // Host can't have more than 63 characters
      it("should complain when host has more than 63 characters") {
        Validation.isValidHost("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ01") match {
          case Success(_) => fail("Failed validating host which has more than 63 characters")
          case Failure(_) => succeed
        }
      }
      it("should complain when host is in invalid format") {
        Validation.isValidHost("1.1.1.1.1.1") match {
          case Success(_) => fail("Passed invalid host")
          case Failure(_) => succeed
        }
      }
    }

  }

}
