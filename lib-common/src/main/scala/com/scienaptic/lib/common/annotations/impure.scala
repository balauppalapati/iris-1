package com.scienaptic.lib.common.annotations

// scalastyle:off

/**
  * An annotation that designates that a method is an "Impure" function,
  * i.e., that its result depends on something more than its input parameters,
  * or that it has one or more external side effects.
  */
@SuppressWarnings(Array("ClassNames"))
final class impure extends scala.annotation.StaticAnnotation {}

// scalastyle:on
