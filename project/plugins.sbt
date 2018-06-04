/**
  * Create a fat JAR of your project with all of its dependencies.
  */
// https://github.com/sbt/sbt-assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

// App Packaging
// https://github.com/sbt/sbt-native-packager
//addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

/**
  * Create a dependency graph for your project
  */
// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")
// https://github.com/dwijnand/sbt-project-graph
addSbtPlugin("com.dwijnand" % "sbt-project-graph" % "0.2.2")


/**
  * Linting tool
  */
// https://github.com/scalacenter/scalafix/issues/463
// https://scalacenter.github.io/scalafix/
//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.5.6")

/**
  * Scala Static Analysis Tools
  */
// http://www.scalastyle.org/
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
// https://github.com/sksamuel/sbt-scapegoat
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.0.7")
// http://www.wartremover.org/doc/install-setup.html
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.1")

/**
  * Code formatter for scala
  */
// http://scalameta.org/scalafmt/#sbt
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.3.0")

/**
  * Scaldoc api mappings
  */
// https://github.com/ThoughtWorksInc/sbt-api-mappings
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "2.0.1")

/**
  * Scala Code Coverage
  */
// https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

/**
  * Source Code Statistics
  */
// https://github.com/orrsella/sbt-stats
addSbtPlugin("com.orrsella" % "sbt-stats" % "1.0.7")

/**
  * Check maven and ivy repositories for dependency updates
  */
// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.3")

/**
  * Proguard - Optimizer for Java bytecode
  */
// https://github.com/sbt/sbt-proguard
//addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.3.0")

/**
  * Scala Artifact Fetching
  */
// https://github.com/coursier/coursier
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC13")
