package com.scienaptic.lib.datasources.jdbc

import java.sql.SQLException

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.datasources.interfaces.{ DsnConfigFromRequest, DsnConfigParamsType }
import io.vertx.core.json.JsonObject
import io.vertx.scala.core.http.HttpServerRequest

import scala.util.control.NonFatal

/** DSN Data for specific data sources */
trait DsnConfig {

  def listSchemas: Option[String]

  def listTables(schema: Option[String]): Option[String]

  def driverClass: String

  def getJdbcConfig: Option[JdbcConfig]

  def getRow(table: String, condition: String): Option[String]

  def getColumnsMetadataConfig(table: String): MetadataConfig

  def getConnectionErrors(e: Throwable): ErrorMap = e match {
    case x: SQLException => getSQLExceptionErrors(x)
    case NonFatal(_)     => Exceptions.unknownException(e)
  }

  def getSQLExceptionErrors(e: SQLException): ErrorMap
}

trait DsnConfigCompanion {

  def apply(params: DsnConfigParamsType): DsnConfig =
    params match {
      case DsnConfigFromRequest(req) => fromRequest(req)
    }

  def fromRequest(req: HttpServerRequest): DsnConfig

}

case class JdbcConfig(driverClass: String, user: String, password: String, url: String)

object JdbcConfig {

  def getJsonObject(jdbcConfig: JdbcConfig): JsonObject =
    new JsonObject()
      .put("driver_class", jdbcConfig.driverClass)
      .put("user", jdbcConfig.user)
      .put("password", jdbcConfig.password)
      .put("url", jdbcConfig.url)
}

/**
  *
  * @param catalog An abstraction of data storage. Self-contained isolated namespace, but not all SQL engines do it.
  * @param schemaPattern
  * @param tableNamePattern
  * @param columnNamePattern
  */
case class MetadataConfig(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String)
