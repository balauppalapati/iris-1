import sbt.io.Using
import sbt.{Resolver, _}
import complete.DefaultParsers.spaceDelimited

// sbt-scapegoat
scapegoatVersion in ThisBuild := "1.3.4"

// sbt-scoverage
coverageHighlighting := true
coverageEnabled := true

// sbt-proguard
//enablePlugins(SbtProguard)
//proguardInputs in Proguard := Seq(baseDirectory.value / "target" / s"scala-${scalaVersion.value.dropRight(2)}" / s"${name.value}-${version.value}.jar")
//proguardLibraries in Proguard := Seq()
//proguardInputFilter in Proguard := { file => None }
//proguardMerge in Proguard := false
//(proguard in Proguard) := (proguard in Proguard).dependsOn(assembly).value

val commonSettings = Seq(
  version := "0.0.1",
  organization := "com.scienaptic",
  scalaVersion := Version.Scala_2_12,
  // http://www.scala-sbt.org/1.x/docs/Library-Dependencies.html#Resolvers
  // http://www.scala-sbt.org/1.x/docs/Resolvers.html
  resolvers += Resolver.mavenLocal,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-project-maven.html
  // Redshift
  resolvers += "redshift" at "http://redshift-maven-repository.s3-website-us-east-1.amazonaws.com/release",
  // DB2
  resolvers += "Alfresco Public" at "https://artifacts.alfresco.com/nexus/content/repositories/public/",
  // Hadrian
  resolvers += "opendatagroup" at "http://repository.opendatagroup.com/maven",
  // Wart remover config
  //    wartremoverWarnings ++= Warts.allBut(
  //  Wart.DefaultArguments,
  //  Wart.NonUnitStatements
  //),
)

val assemblySettings = Seq(
  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs@_*) => xs map {
      _.toLowerCase
    } match {
      case ("manifest.mf" :: Nil) => MergeStrategy.discard
      case ("license" :: Nil) => MergeStrategy.discard
      // https://stackoverflow.com/questions/34855649/
      // For mssql-jdbc
      case ps@(_ :: _) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") =>
        MergeStrategy.discard
      case _ => MergeStrategy.last
    }
    case PathList("codegen.json") => MergeStrategy.discard
    // https://github.com/vert-x3/vertx-lang-scala/issues/64
    case PathList("vertx-service-discovery-js", xs@_*) => MergeStrategy.first
    // https://groups.google.com/forum/#!topic/vertx-dev/6LpyCOyVImE
    case PathList("io", "vertx", "groovy", "ext", "web", _*) => MergeStrategy.last
    case PathList("io", "vertx", "ext", "web", _*) => MergeStrategy.last
    // Netty
    case PathList("javax", "servlet", _*) => MergeStrategy.last
    // "org.apache.hadoop" % "hadoop-common"
    case PathList("org", "apache", "commons", "beanutils", _*) => MergeStrategy.last
    case PathList("org", "apache", "commons", "collections", _*) => MergeStrategy.last
    // "org.apache.poi" % "poi-ooxml"
    case PathList("javax", "xml", "stream", _*) => MergeStrategy.last
    case PathList("javax", "xml", "namespace", _*) => MergeStrategy.last
    case PathList("javax", "xml", "XMLConstants.class") => MergeStrategy.last
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

val libSettings = commonSettings ++ assemblySettings ++ Seq(
  libraryDependencies ++= Seq(
    Library.ScalaTest,
  )
)

val commonAppSettings = libSettings ++ Seq(
  libraryDependencies ++= Seq(
    Library.VertxWeb
  ) ++ Seq(Library.Logging).flatten
)

def manifest(project: String): java.util.jar.Manifest = {
  val file = new java.io.File(project + "/src/META-INF/MANIFEST.MF")
  Using.fileInputStream(file)(in => new java.util.jar.Manifest(in))
}

/**
  * --------------------------------------------------------------------------------------------------------------------
  * Library project Settings
  * --------------------------------------------------------------------------------------------------------------------
  */

// Don't add any libraries to this sub project.
lazy val libCommon = (project in file("lib-common"))
  .settings(libSettings: _*)
  .settings(
    name := "lib-common"
  )

lazy val libValidation = (project in file("lib-validation"))
  .settings(libSettings: _*)
  .settings(
    name := "lib-validation",
    libraryDependencies ++= Seq(
      Library.IPAddress
    )
  )

lazy val libDataSources = (project in file("lib-data-sources"))
  .dependsOn(libCommon)
  .settings(libSettings: _*)
  .settings(
    name := "lib-data-sources",
    libraryDependencies ++= Seq(
      Library.Enumeratum,
      Library.VertxJdbcClient
    ) ++ Seq(Library.DataSources).flatten
  )

lazy val libFileSources = (project in file("lib-file-sources"))
  .dependsOn(libCommon)
  .settings(libSettings: _*)
  .settings(
    name := "lib-file-sources",
    libraryDependencies ++= Seq(
      Library.Enumeratum,
      Library.VertxLangScala,
    ) ++ Seq(Library.FileSources).flatten
  )

lazy val libSpark = (project in file("lib-spark"))
  .settings(libSettings: _*)
  .settings(scalaVersion := Version.Scala_2_11)
  .settings(
    name := "lib-spark",
    libraryDependencies ++= Seq(
      Library.Mist,
    ) ++ Seq(Library.Spark).flatten
  )

lazy val libPfa = (project in file("lib-pfa"))
  .settings(libSettings: _*)
  .settings(
    name := "lib-pfa",
    libraryDependencies ++= Seq(
      Library.Hadrian,
    )
  )

/**
  * --------------------------------------------------------------------------------------------------------------------
  * App project Settings
  * --------------------------------------------------------------------------------------------------------------------
  */
lazy val common = (project in file("common"))
  .dependsOn(libCommon)
  .settings(commonAppSettings: _*)
  .settings(
    name := "common",
    libraryDependencies ++= Seq(
      Library.VertxCircuitBreaker
    ) ++ Seq(
      Library.VertxServiceDiscovery,
      Library.Circe
    ).flatten
  )

lazy val dataSources = (project in file("data-sources"))
  .dependsOn(libCommon, libDataSources, libFileSources, common)
  .settings(commonAppSettings: _*)
  .settings(
    name := "data-sources",
    libraryDependencies ++= Seq(

    ),
    packageOptions += {
      Package.JarManifest(manifest("data-sources"))
    }
  )

lazy val apiGateway = (project in file("api-gateway"))
  .dependsOn(libCommon, common)
  .settings(commonAppSettings: _*)
  .settings(
    name := "api-gateway",
    libraryDependencies ++= Seq(
      Library.VertxWebClient,
    ),
    packageOptions += {
      Package.JarManifest(manifest("api-gateway"))
    }
  )

/**
  * --------------------------------------------------------------------------------------------------------------------
  * SBT tasks
  * --------------------------------------------------------------------------------------------------------------------
  */
// Run in sbt console like `assemblyWith dataSources`
lazy val assemblyWith =
inputKey[Unit]("assembly sub-projects specified in args")
assemblyWith := {
  val args = spaceDelimited("<arg>").parsed
  val curState = state.value
  val log = curState.globalLogging.full
  //  val extractedRoot = sbt.Project.extract(curState)
  //  val destDirectory = (crossTarget in extractedRoot.currentRef get extractedRoot.structure.data).get
  args
    .collect {
      case projectName if (file(".") / projectName).exists =>
        ProjectRef(file(".") / projectName, projectName)
    }
    .foreach { projectRef =>
      log.info(s"managedProject: $projectRef")
      val assemblyJarFile = projectRef.project match {
        case "apiGateway" =>
          assembly.all(ScopeFilter(inProjects(apiGateway))).value.head
        case "dataSources" =>
          assembly.all(ScopeFilter(inProjects(dataSources))).value.head
      }
      log.info(s"out: ${assemblyJarFile.getCanonicalPath}")
      //    IO.copyFile(assemblyJarFile, destDirectory)
    }
}
