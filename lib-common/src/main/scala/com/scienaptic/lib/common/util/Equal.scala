package com.scienaptic.lib.common.util

object Equal {

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  implicit final class EqualHelper[A](self: A) {

    /**
      * Check for equality.
      * Use this instead of <b>==</b> for type safety.
      *
      * @param other
      * @return
      */
    def ===(other: A): Boolean = self == other // scalastyle:ignore

    /**
      * Check for inequality.
      * Use this instead of <b>!=</b> for type safety.
      *
      * @param other
      * @return
      */
    def =/=(other: A): Boolean = self != other // scalastyle:ignore
  }

}
