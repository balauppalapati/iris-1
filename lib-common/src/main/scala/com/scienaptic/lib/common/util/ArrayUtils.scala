package com.scienaptic.lib.common.util

import com.scienaptic.lib.common.annotations.impure

object ArrayUtils {

  /**
    * Returns random element from an Array.
    */
  @impure def getRandomElement[T](xs: Array[T]): Option[T] =
    if (xs.isEmpty) None else xs.lift(util.Random.nextInt(xs.length))
}
