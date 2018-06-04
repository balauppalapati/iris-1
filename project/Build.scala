import sbt.Keys._
import sbt._

object Build extends AutoPlugin {

  // Triggers this auto plugin automatically
  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: scala.Seq[sbt.Def.Setting[_]] =
    Vector(
      version := version.in(ThisBuild).value,
      scalaVersion := Version.Scala_2_12,
      crossScalaVersions := Vector(scalaVersion.value),
      scalacOptions ++= Vector(
        "-unchecked", // Enable additional warnings where generated code depends on assumptions
        "-deprecation", // Emit warning and location for usages of deprecated APIs
        "-feature", // Emit warning and location for usages of features that should be imported explicitly
        "-Xlint",   // Linting checks
        // "-Xfatal-warnings", // Turn warnings into errors
        // "-Ywarn-macros:after", // To remove false positive warning on unused implicit val
        "-language:_",
        "-target:jvm-1.8",
        "-encoding",
        "UTF-8"
      ),
      unmanagedSourceDirectories in Compile := Vector(
        javaSource.in(Compile).value,
        scalaSource.in(Compile).value
      ),
      unmanagedSourceDirectories in Test := Vector(scalaSource.in(Test).value),
      initialCommands in console :=
        """|import io.vertx.lang.scala._
           |import io.vertx.lang.scala.ScalaVerticle.nameForVerticle
           |import io.vertx.scala.core._
           |import scala.concurrent.Future
           |import scala.concurrent.Promise
           |import scala.util.Success
           |import scala.util.Failure
           |val vertx = Vertx.vertx
           |implicit val executionContext = io.vertx.lang.scala.VertxExecutionContext(vertx.getOrCreateContext)
           |""".stripMargin
    )

}
