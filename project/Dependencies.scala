import sbt._

object Version {
  final val Scala_2_12 = "2.12.5"
  final val Scala_2_11 = "2.11.12"
  final val Vertx      = "3.5.1"
  final val Circe      = "0.9.2"
  final val Excel      = "3.17"
  final val Hadoop     = "2.9.0"
  final val Spark      = "2.3.0"
}

object Library {

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Vert.x
    * ------------------------------------------------------------------------------------------------------------------
    */
  final val VertxLangScala = "io.vertx" %% "vertx-lang-scala" % Version.Vertx
  final val VertxWeb       = "io.vertx" %% "vertx-web-scala"  % Version.Vertx withSources ()

  final val VertxServiceDiscovery = Seq(
    "io.vertx" %% "vertx-service-discovery-scala" % Version.Vertx withSources ()
    //    "io.vertx" % "vertx-service-discovery-backend-consul" % Version.Vertx withSources()
  )
  final val VertxCircuitBreaker = "io.vertx" %% "vertx-circuit-breaker-scala" % Version.Vertx withSources ()

  final val VertxWebClient = "io.vertx" %% "vertx-web-client-scala" % Version.Vertx withSources ()

  final val VertxJdbcClient = "io.vertx" %% "vertx-jdbc-client-scala" % Version.Vertx withSources ()

  //  final val VertxDropwizardMetrics = "io.vertx" %% "vertx-dropwizard-metrics-scala" % Version.Vertx withSources()
  //  final val VertxCodegen = "io.vertx" % "vertx-codegen" % Version.Vertx % "provided" withSources()

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Logging
    * ------------------------------------------------------------------------------------------------------------------
    */
  // Log4j2 logging
  // https://github.com/apache/logging-log4j-scala
  final val Logging = Seq(
    "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
    "org.apache.logging.log4j" % "log4j-api"        % "2.11.0",
    "org.apache.logging.log4j" % "log4j-core"       % "2.11.0" % Runtime
  )

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Data Sources
    * ------------------------------------------------------------------------------------------------------------------
    */
  //  private final val Oracle = "com.oracle.jdbc" % "ojdbc8" % "12.2.0.1" // Unmanaged
  private final val DB2        = "com.ibm.db2.jcc"         % "db2jcc4"              % "10.1"
  private final val MsSQL      = "com.microsoft.sqlserver" % "mssql-jdbc"           % "6.4.0.jre8"
  private final val MySQL      = "mysql"                   % "mysql-connector-java" % "8.0.9-rc"
  private final val PostgreSQL = "org.postgresql"          % "postgresql"           % "42.2.2"
  private final val Redshift   = "com.amazon.redshift"     % "redshift-jdbc42"      % "1.2.12.1017"

  private final val DataWorld = "world.data" % "dw-jdbc" % "0.4.2"

  final val DataSources = Seq(DB2, MsSQL, MySQL, PostgreSQL, Redshift) ++ Seq(DataWorld)

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * File Sources
    * ------------------------------------------------------------------------------------------------------------------
    */
  // MIME Type detector

  private final val ApacheTika = "org.apache.tika" % "tika-core" % "1.17"

  // Compressors & Archivers

  private final val ApacheCommonsCompress = "org.apache.commons" % "commons-compress" % "1.16.1"
  // For LZMA format in Commons Compress.
  private final val Xz = "org.tukaani" % "xz" % "1.8" // https://tukaani.org/xz/

  // File Formats

  private final val Csv = "com.univocity" % "univocity-parsers" % "2.6.1"

  private final val Excel = Seq("poi", "poi-ooxml")
    .map("org.apache.poi" % _ % Version.Excel)

  // File Systems

  private final val AwsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.301"

  private final val Hadoop = Seq(
    // http://www.scala-sbt.org/1.0/docs/Library-Management.html#Exclude+Transitive+Dependencies
    // https://github.com/netty/netty/issues/4671
    "org.apache.hadoop" % "hadoop-hdfs" % Version.Hadoop excludeAll ExclusionRule(organization = "io.netty",
                                                                                  name = "netty-all"),
    "org.apache.hadoop" % "hadoop-common" % Version.Hadoop
  )

  final val FileSources = Seq(ApacheTika, ApacheCommonsCompress, Xz, Csv, AwsS3) ++ Seq(Excel, Hadoop).flatten

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Spark
    * ------------------------------------------------------------------------------------------------------------------
    */
  final val Mist = "io.hydrosphere" %% "mist-lib" % "1.0.0-RC13"

  final val Spark = Seq("spark-core", "spark-sql", "spark-hive", "spark-streaming", "spark-mllib")
    .map("org.apache.spark" %% _ % Version.Spark % "provided")

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Validation
    * ------------------------------------------------------------------------------------------------------------------
    */
  final val IPAddress = "com.github.seancfoley" % "ipaddress" % "4.1.0"

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Libraries
    * ------------------------------------------------------------------------------------------------------------------
    */
  // https://github.com/lloydmeta/enumeratum
  // Replacement for scala Enum.
  final val Enumeratum = "com.beachape" %% "enumeratum" % "1.5.13"

  // https://github.com/circe/circe
  // Json library for scala
  final val Circe = Seq("circe-core", "circe-generic", "circe-parser").map("io.circe" %% _ % Version.Circe)

  final val Hadrian = "com.opendatagroup" % "hadrian" % "0.8.3"

  /**
    * ------------------------------------------------------------------------------------------------------------------
    * Testing
    * ------------------------------------------------------------------------------------------------------------------
    */
  final val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"

  //  final val ScalaCheck = "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
}
