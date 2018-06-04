package com.scienaptic.lib.datasources.jdbc

import java.sql.SQLException

import com.mchange.v2.resourcepool.CannotAcquireResourceException
import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.DataSource
import io.vertx.scala.core.http.HttpServerRequest

import scala.util.control.NonFatal

/**
  * Driver config for [[com.scienaptic.lib.datasources.DataSource.Redshift]]
  *
  * @param user
  * @param password
  * @param host
  * @param port
  * @param database
  * @param ssl
  */
final case class Redshift(user: String,
                          password: String,
                          host: String,
                          port: Int,
                          database: String,
                          ssl: Boolean,
                          schema: Option[String])
  extends DsnConfig {

  def driverClass: String = "com.amazon.redshift.jdbc.Driver"

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    Some(JdbcConfig(driverClass, user, password, s"jdbc:redshift://$host:$port/$database?ssl=$ssl"))

  /**
    * Server instance == db cluster == all data managed by same execution engine.
    * Database == Catalog == single database within db cluster, isolated from other databases in same db cluster
    * Schema == namespace within database
    * User == named account, who can connect to database, own and use objects in each allowed database separately
    * To identify any object in running server, you need (database name + schema name + object name)
    *
    * @param table
    * @return
    */
  def getColumnsMetadataConfig(table: String): MetadataConfig = MetadataConfig("", "", table, "")

  /**
    * query string for fetching listOfSchemas
    */
  val listSchemas = Some(s" SELECT NSPNAME FROM PG_NAMESPACE ")

  /**
    *
    * @param schema
    * @return
    */
  def listTables(schema: Option[String]): Option[String] = schema match {
    case Some(schm) =>
      Some(
        s" SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema='${schm}'"
      )
    case None =>
      Some(
        s" SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema='public' "
      )
  }

  /**
    *
    * @param table
    * @param condition
    * @return query that returns any ONE row from RedShift table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val where =
      if (condition.isEmpty) " "
      else s" WHERE $condition "

    val from = schema match {
      case Some(x) => s" FROM $x.$table "
      case None => s" FROM $table "
    }

    Some(s"SELECT * $from $where LIMIT 1 ")
  }

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = Exceptions.getCause(e) match {
    case Some(x: CannotAcquireResourceException) => getCommunicationErrors(x)
    case Some(NonFatal(x)) => Exceptions.unknownException(x)
    case None => Exceptions.unknownException(e)
  }

  /**
    * Errors on a communicating with database.
    */
  private def getCommunicationErrors(e: CannotAcquireResourceException): ErrorMap = Exceptions.getCause(e) match {
    case Some(x: SQLException) =>
      Exceptions.getCause(x) match {
        case Some(y) =>
          x.getSQLState match {
            case "28000" => // invalid_authorization_specification
              Map(s"${Redshift.K.User}|${Redshift.K.Password}" -> y.getMessage)
            case "3D000" => // invalid_catalog_name
              Map(Redshift.K.Database -> y.getMessage)
            case "HY000" =>
              Map(Exceptions.Form_Error -> y.getMessage)
            case _ => Exceptions.unknownException(y)
          }
        case None => Exceptions.unknownException(x)
      }
    case Some(NonFatal(x)) => Exceptions.unknownException(x)
    case None => Exceptions.unknownException(e)
  }

}

object Redshift extends DsnConfigCompanion {

  // Keys used in request
  private object K {
    // Dsn
    final val User: String = "user"
    final val Password: String = "password"
    final val Host: String = "host"
    final val Port: String = "port"
    final val Database: String = "database"
    final val Ssl: String = "ssl"
    final val Schema: String = "schema"
    // Metadata
    final val Table: String = "table"
  }

  def fromRequest(req: HttpServerRequest): Redshift = {

    val r = req.getParam _

    val user = r(K.User)
    val password = r(K.Password)
    val host = r(K.Host) orElse DataSource.Redshift.host
    val port = r(K.Port).toInt.toOption.flatten orElse DataSource.Redshift.port
    val database = r(K.Database)
    val ssl = r(K.Ssl).toBoolean.toOption.flatten orElse DataSource.Redshift.ssl
    val schema = r(K.Schema)

    Redshift(user.get, password.get, host.get, port.get, database.get, ssl.get, schema)
  }

}
