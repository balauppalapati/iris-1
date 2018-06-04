package com.scienaptic.lib.common.util

import org.scalatest.FunSpec

import scala.annotation.tailrec

class ArrayUtilsTest extends FunSpec {

  private def getRandomValues[T](xs: Array[T], iterations: Int): Array[Option[T]] = {

    @tailrec
    def loop(values: Array[Option[T]], iteration: Int): Array[Option[T]] =
      if (iteration == 0) {
        values
      } else {
        loop(values :+ ArrayUtils.getRandomElement(xs), iteration - 1)
      }

    loop(Array(), iterations)
  }

  describe("Get random element from array") {
    it("should provide random values on each call") {
      val iterations  = 100
      val sampleArray = Array(1, 2, 3, 4, 5)
      if (getRandomValues(sampleArray, iterations).distinct.length == 1) {
        fail("Failed: Same values are present in the collected random sample")
      } else {
        succeed
      }
    }

    describe("(when empty)") {
      it("should return None") {
        ArrayUtils.getRandomElement(Array()) match {
          case None => succeed
          case _    => fail("Failed: Not returning None for empty array")
        }
      }
    }
  }

}
