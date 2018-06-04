package com.scienaptic.datasources

import com.scienaptic.common.BaseHttpServerVerticle
import com.scienaptic.common.util.JsonEncoder
import com.scienaptic.common.util.JsonObjectImplicit.JsonObjectHelper
import com.scienaptic.datasources.config.{ Config, Constants }
import com.scienaptic.lib.common.annotations.impure
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.interfaces.DsnConfigFromRequest
import com.scienaptic.lib.datasources.jdbc.JdbcConfig
import com.scienaptic.lib.datasources.{ DataSource, Metadata, SQLClient }
import com.scienaptic.lib.filesources.Storage
import com.scienaptic.lib.filesources.filesource.{ Csv, Excel }
import com.scienaptic.lib.filesources.inputstream.InputStreamConfig
import com.scienaptic.lib.filesources.interfaces.InputStreamConfigFromRequest
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.vertx.scala.ext.web.RoutingContext

final class DataSourcesAPIVerticle extends BaseHttpServerVerticle {

  @impure override def start(): Unit = {

    super.start()

    val router = createRouter(vertx)

    // Attach handlers to routes
    router.get("/csv").handler(parseCsv)
    router.get("/excel").handler(parseExcel)
    router.get("/jsonFile").handler(jsonFile)

    router.get(Constants.Endpoints.DataSources).handler(dataSources)
    router.get(Constants.Endpoints.DatabasesInDataSources).handler(databasesInDataSources)
    router.get(Constants.Endpoints.TablesInDataSources).handler(tablesInDataSources)
    router.get(Constants.Endpoints.ColumnsWithMetadataInDataSources).handler(columnsWithMetadataInDataSources)
    router.get(Constants.Endpoints.RowFromTable).handler(rowFromTable)
    router.get(Constants.Endpoints.SchemasInDatabase).handler(schemasInDatabases)

    // Setup host and port for this service
    val host = config.getString(Config.Http.Host)
    val port = config.getInteger(Config.Http.Port)

    createAndPublishHttpServerFuture(Constants.ServiceName,
                                     router,
                                     host,
                                     port,
                                     BaseHttpServerVerticle.HttpEndpointRestApi)
  }

  /**
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def parseExcel(ctx: RoutingContext): Unit = {

    val req = ctx.request

    def getInputStreamFuture(inputStreamData: InputStreamConfig) = inputStreamData.getInputStreamFuture

    val result = for {

      source          <- req.getParam("source")
      storage         <- Storage.withNameInsensitiveOption(source)
      inputStreamData <- storage.inputStreamConfig(InputStreamConfigFromRequest(req))

    } yield
      for {
        excelRows <- Excel.getAttributesFuture(getInputStreamFuture(inputStreamData)) andThen
                    errors(ctx, Excel.getAttributesErrors(inputStreamData.getInputStreamErrors))
      } yield {
        ok(ctx, ParseExcel.response(excelRows).asJson.noSpaces)
      }

    result match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source"))
    }
  }

  /**
    * Parse csv file to find its attributes like delimiter, line separator, quote and escape characters.
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def parseCsv(ctx: RoutingContext): Unit = {

    val req = ctx.request

    def getInputStreamFuture(inputStreamData: InputStreamConfig) = inputStreamData.getInputStreamFuture

    val result = for {

      source          <- req.getParam("source")
      storage         <- Storage.withNameInsensitiveOption(source)
      inputStreamData <- storage.inputStreamConfig(InputStreamConfigFromRequest(req))

    } yield {

      val maxColumns            = req.getParam("maxColumns").toInt.toOption.flatten
      val maxCharsPerColumn     = req.getParam("maxCharsPerColumn").toInt.toOption.flatten
      val numberOfRecordsToRead = req.getParam("numberOfRecordsToRead").toInt.toOption.flatten
      val parserSettings        = Csv.getCsvParseSettings(maxColumns, maxCharsPerColumn, numberOfRecordsToRead)

      for {
        csvFormatWithRows <- Csv.getAttributesFuture(getInputStreamFuture(inputStreamData), parserSettings) andThen
                            errors(ctx, Csv.getAttributesErrors(inputStreamData.getInputStreamErrors))
      } yield {
        ok(ctx, ParseCsv.response(csvFormatWithRows).asJson.noSpaces)
      }
    }

    result match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source"))
    }

  }

  /**
    * Get list of sources supported
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def dataSources(ctx: RoutingContext): Unit =
    ok(ctx, Sources.sources.asJson.noSpaces)

  /**
    * Get list of databases for provided data source(:source)
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def databasesInDataSources(ctx: RoutingContext): Unit = {

    val req = ctx.request

    val result = for {
      source     <- req.getParam("source")
      dataSource <- DataSource.withNameInsensitiveOption(source)
      dsnConfig  <- dataSource.dsnConfig(DsnConfigFromRequest(req))
      jdbcConfig <- dsnConfig.getJdbcConfig
      sql        <- dataSource.listDatabases
    } yield
      for {
        json <- new SQLClient(vertx, JdbcConfig.getJsonObject(jdbcConfig)).queryColumnFuture(sql) andThen
               errors(ctx, dsnConfig.getConnectionErrors)
      } yield ok(ctx, json.encode)

    result match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source"))
    }
  }

  /**
    * Get list of databases for provided data source(:source)
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def schemasInDatabases(ctx: RoutingContext): Unit = {

    val req = ctx.request

    val result = for {
      source     <- req.getParam("source")
      dataSource <- DataSource.withNameInsensitiveOption(source)
      dsnConfig  <- dataSource.dsnConfig(DsnConfigFromRequest(req))
      jdbcConfig <- dsnConfig.getJdbcConfig
      sql        <- dsnConfig.listSchemas
    } yield
      for {
        json <- new SQLClient(vertx, JdbcConfig.getJsonObject(jdbcConfig)).queryColumnFuture(sql) andThen
               errors(ctx, dsnConfig.getConnectionErrors)
      } yield ok(ctx, json.encode)

    result match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source"))
    }
  }

  /**
    * Get list of tables for provided data source(:source)
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def tablesInDataSources(ctx: RoutingContext): Unit = {

    val req = ctx.request

    val result = for {
      source     <- req.getParam("source")
      dataSource <- DataSource.withNameInsensitiveOption(source)
      dsnConfig  <- dataSource.dsnConfig(DsnConfigFromRequest(req))
      jdbcConfig <- dsnConfig.getJdbcConfig
      sql        <- dsnConfig.listTables(req.getParam("schema"))
    } yield
      for {
        json <- new SQLClient(vertx, JdbcConfig.getJsonObject(jdbcConfig)).queryColumnFuture(sql) andThen
               errors(ctx, dsnConfig.getConnectionErrors)
      } yield ok(ctx, json.encode)

    result match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source"))
    }
  }

  /**
    * Get list of columns with its metadata for provided data source(:source)
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def columnsWithMetadataInDataSources(ctx: RoutingContext): Unit = {

    implicit val encodeNull: Encoder[String] = JsonEncoder.nullAsEmptyString

    val req   = ctx.request
    val table = req.getParam("table").get

    val result = for {
      source     <- req.getParam("source")
      dataSource <- DataSource.withNameInsensitiveOption(source)
      dsnConfig  <- dataSource.dsnConfig(DsnConfigFromRequest(req))
    } yield
      for {
        metadataFuture <- Metadata.getColumnsFuture(dsnConfig, table) andThen
                         errors(ctx, dsnConfig.getConnectionErrors)
      } yield
        for {
          metadata <- metadataFuture
        } ok(ctx, metadata.asJson.noSpaces)

    result match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source"))
    }
  }

  /**
    *
    * @param ctx Context for the handling of a request
    */
  @impure private def jsonFile(ctx: RoutingContext): Unit = {

    val response = ctx.response.putHeader("content-type", "application/json")

    vertx.fileSystem
      .readFileFuture("sample.json")
      .map(buffer => buffer.toJsonObject)
      .map { json =>
        response.end(json.encode)
      }
  }

  /**
    *
    * @param ctx Context for handling the request
    */
  @impure private def rowFromTable(ctx: RoutingContext): Unit = {

    val req = ctx.request();

    val row = for {
      source     <- req.getParam("source")
      table      <- req.getParam("table")
      condition  <- req.getParam("condition")
      dataSource <- DataSource.withNameInsensitiveOption(source) //
      dsnConfig  <- dataSource.dsnConfig(DsnConfigFromRequest(req))
      jdbcConfig <- dsnConfig.getJdbcConfig
      sql        <- dsnConfig.getRow(table, condition)
    } yield
      for {
        json <- new SQLClient(vertx, JdbcConfig.getJsonObject(jdbcConfig)).queryColumnFuture(sql) andThen
               errors(ctx, dsnConfig.getConnectionErrors)
      } yield ok(ctx, json.encode)

    row match {
      // Empty case. Error is handled in for comprehension of futures.
      case Some(_) =>
      case None    => fail(ctx, Map("source" -> "Invalid source/table/condition"))
    }

  }

}
