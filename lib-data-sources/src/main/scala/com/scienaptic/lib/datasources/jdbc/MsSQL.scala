package com.scienaptic.lib.datasources.jdbc

import java.sql.SQLException

import com.microsoft.sqlserver.jdbc.SQLServerException
import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.Equal._
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.DataSource
import io.vertx.scala.core.http.HttpServerRequest

/**
  * Driver config for [[com.scienaptic.lib.datasources.DataSource.MsSQL]]
  *
  * @return
  */
final case class MsSQL(user: String,
                       password: String,
                       host: String,
                       port: Int,
                       database: String,
                       ssl: Boolean,
                       loginTimeoutSeconds: Int,
                       socketTimeoutSeconds: Int,
                       schema: Option[String])
    extends DsnConfig {

  def driverClass: String = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    Some(
      JdbcConfig(
        driverClass,
        user,
        password,
        s"jdbc:sqlserver://$host:$port;databaseName=$database;loginTimeout=$loginTimeoutSeconds;socketTimeout=$socketTimeoutSeconds;"
      )
    )

  /**
    * Server instance == set of managed databases
    * Database == namespace qualifier within the server, rarely referred to as catalog
    * Schema == Owner == namespace within the database, tied to database roles, by default only dbo is used
    * User == named account, who can connect to server and use (but can not own - schema works as owner)
    * objects in one or more databases
    * To identify any object in running server, you need (database name + owner + object name)
    *
    * @param table
    * @return
    */
  def getColumnsMetadataConfig(table: String): MetadataConfig =
    MetadataConfig(null, null, table, null) // scalastyle:ignore

  /**
    * query string for fetching listOfSchemas
    */
  val listSchemas = Some(s" SELECT name FROM sys.schemas ")

  def listTables(schema: Option[String]): Option[String] = schema match {
    case Some(x) => Some(s"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$x'")
    case None    => Some("SELECT name FROM sys.Tables")
  }

  /**
    * @param table
    * @param condition
    * @return query that returns any ONE row from MsSQL table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val where =
      if (condition.isEmpty) " "
      else s" WHERE $condition "

    val from = schema match {
      case Some(x) => s" FROM $x.$table "
      case None    => s" FROM $table"
    }

    Some(s"SELECT TOP(1) *  $from $where ")
  }

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = {
    val errorCode = e.getErrorCode
    if (errorCode =/= 0) {
      errorCode match {
        case ErrorCodes.LOGON_FAILED =>
          Map(s"${MsSQL.K.User}|${MsSQL.K.Password}" -> e.getMessage)
        case ErrorCodes.PASSWORD_EXPIRED =>
          Map(MsSQL.K.Password -> e.getMessage)
        case ErrorCodes.DATABASE_NAME_UNDEFINED =>
          Map(MsSQL.K.Database -> e.getMessage)
        case _ =>
          Exceptions.unknownException(e)
      }
    } else {
      getConnectionErrorsNoVendorCode(e)
    }
  }

  /**
    * Errors on a database connection
    */
  private def getConnectionErrorsNoVendorCode(e: SQLException): ErrorMap = e match {
    case _: SQLServerException =>
      e.getSQLState match {
        case "08S01" => Map(s"${MsSQL.K.Host}|${MsSQL.K.Port}" -> e.getMessage)
        case _       => Exceptions.unknownException(e)
      }
    case _ => Exceptions.unknownException(e)
  }

  private object ErrorCodes {
    final val LOGON_FAILED: Int            = 18456
    final val PASSWORD_EXPIRED: Int        = 18488
    final val DATABASE_NAME_UNDEFINED: Int = 4060
  }

}

object MsSQL extends DsnConfigCompanion {

  final val LOGIN_TIMEOUT_IN_SECONDS  = 15
  final val SOCKET_TIMEOUT_IN_SECONDS = 0

  // Keys used in request
  private object K {
    // Dsn
    final val User: String          = "user"
    final val Password: String      = "password"
    final val Host: String          = "host"
    final val Port: String          = "port"
    final val Database: String      = "database"
    final val Ssl: String           = "ssl"
    final val LoginTimeout: String  = "loginTimeout"
    final val SocketTimeout: String = "socketTimeout"
    final val Schema: String        = "schema"
    // Metadata
    final val Table: String = "table"
  }

  def fromRequest(req: HttpServerRequest): MsSQL = {

    val r = req.getParam _

    val user     = r(K.User)
    val password = r(K.Password)
    val host     = r(K.Host) orElse DataSource.MsSQL.host
    val port     = r(K.Port).toInt.toOption.flatten orElse DataSource.MsSQL.port
    val database = r(K.Database) orElse DataSource.MySQL.database

    val ssl           = r(K.Ssl).toBoolean.toOption.flatten orElse DataSource.MsSQL.ssl
    val loginTimeout  = r(K.LoginTimeout).toInt.toOption.flatten getOrElse LOGIN_TIMEOUT_IN_SECONDS
    val socketTimeout = r(K.SocketTimeout).toInt.toOption.flatten getOrElse SOCKET_TIMEOUT_IN_SECONDS
    val schema        = r(K.Schema)

    MsSQL(user.get, password.get, host.get, port.get, database.get, ssl.get, loginTimeout, socketTimeout, schema)
  }

}
