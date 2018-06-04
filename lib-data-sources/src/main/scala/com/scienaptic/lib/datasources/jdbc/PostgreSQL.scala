package com.scienaptic.lib.datasources.jdbc

import java.sql.SQLException

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.DataSource
import io.vertx.scala.core.http.HttpServerRequest

import scala.util.control.NonFatal

/**
  * Driver config for [[com.scienaptic.lib.datasources.DataSource.PostgreSQL]]
  *
  * @param user
  * @param password
  * @param host
  * @param port
  * @param database
  * @param ssl
  * @return
  */
final case class PostgreSQL(user: String,
                            password: String,
                            host: String,
                            port: Int,
                            database: String,
                            ssl: Boolean,
                            schema: Option[String])
    extends DsnConfig {

  def driverClass: String = "org.postgresql.Driver"

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    Some(JdbcConfig(driverClass, user, password, s"jdbc:postgresql://$host:$port/$database?ssl=$ssl"))

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
  def getColumnsMetadataConfig(table: String): MetadataConfig = MetadataConfig("", "", table.toLowerCase, "")

  /**
    *  query string for fetching listOfSchemas
    */
  val listSchemas = Some(s" SELECT NSPNAME FROM PG_NAMESPACE ")

  /**
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
    * @return  query that returns any ONE row from PostgreSQL table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val where =
      if (condition.isEmpty) " "
      else s" WHERE $condition "

    val from = schema match {
      case Some(x) => s" FROM $x.$table "
      case None    => s" FROM $table "
    }

    Some(s"SELECT * $from $where LIMIT 1 ")
  }

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = e.getSQLState match {
    // https://www.postgresql.org/docs/9.4/static/errcodes-appendix.html
    case "08001" => // sqlclient_unable_to_establish_sqlconnection
      Exceptions.getCause(e) match {
        case Some(_: java.net.UnknownHostException) => Map(PostgreSQL.K.Host -> s"Unknown host: $host")
        case Some(x: java.net.ConnectException)     => Map(s"${PostgreSQL.K.Host}|${PostgreSQL.K.Port}" -> x.getMessage)
        case Some(NonFatal(x))                      => Exceptions.unknownException(x)
        case None                                   => Exceptions.unknownException(e)
      }
    case "28000" => // invalid_authorization_specification
      Map(s"${PostgreSQL.K.User}|${PostgreSQL.K.Password}" -> e.getMessage)
    case "3D000" => // invalid_catalog_name
      Map(PostgreSQL.K.Database -> e.getMessage)
    case "53300" | "HY000" => // too_many_connections | general error
      Map(Exceptions.Form_Error -> e.getMessage)
    case _ => Exceptions.unknownException(e)

  }

}

object PostgreSQL extends DsnConfigCompanion {

  // Keys used in request
  private object K {
    final val User: String     = "user"
    final val Password: String = "password"
    final val Host: String     = "host"
    final val Port: String     = "port"
    final val Database: String = "database"
    final val Ssl: String      = "ssl"
    final val Schema: String   = "schema"
  }

  def fromRequest(req: HttpServerRequest): PostgreSQL = {

    val r = req.getParam _

    val user     = r(K.User)
    val password = r(K.Password)
    val host     = r(K.Host) orElse DataSource.PostgreSQL.host
    val port     = r(K.Port).toInt.toOption.flatten orElse DataSource.PostgreSQL.port
    val database = r(K.Database) orElse DataSource.PostgreSQL.database
    val ssl      = r(K.Ssl).toBoolean.toOption.flatten orElse DataSource.PostgreSQL.ssl
    val schema   = r(K.Schema)

    PostgreSQL(user.get, password.get, host.get, port.get, database.get, ssl.get, schema)
  }

}
