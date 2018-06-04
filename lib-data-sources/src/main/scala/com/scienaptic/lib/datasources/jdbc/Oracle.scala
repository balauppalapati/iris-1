package com.scienaptic.lib.datasources.jdbc

import java.sql.SQLException

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.DataSource
import io.vertx.scala.core.http.HttpServerRequest

/**
  * Driver config for [[com.scienaptic.lib.datasources.DataSource.Oracle]]
  *
  * @param user
  * @param password
  * @param host
  * @param port
  * @param sid
  */
final case class Oracle(user: String,
                        password: String,
                        host: String,
                        port: Int,
                        sid: Option[String],
                        service: Option[String],
                        schema: Option[String])
    extends DsnConfig {

  def driverClass: String = "oracle.jdbc.OracleDriver"

  def urlOption: Option[String] = sid match {
    case Some(sidValue) => Some(s"jdbc:oracle:thin:@$host:$port:$sidValue")
    case None =>
      service match {
        case Some(serviceValue) => Some(s"jdbc:oracle:thin:@$host:$port/$serviceValue")
        case None               => None
      }
  }

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    for {
      url <- urlOption
    } yield JdbcConfig(driverClass, user, password, url)

  /**
    * Server instance == Database == Catalog == all data managed by same execution engine.
    * Schema == Namespace within database, identical to user account
    * User == Schema owner == named account, identical to schema, who can connect to database,
    * who owns the schema and use objects possibly in other schemas
    * To identify any object in running server, you need (schema name + object name)
    *
    * @param table
    * @return
    */
  def getColumnsMetadataConfig(table: String): MetadataConfig =
    MetadataConfig(null, null, table, null) // scalastyle:ignore

  /**
    * query string for fetching listOfSchemas
    */
  val listSchemas = Some("SELECT USERNAME from SYS.ALL_USERS")

  /**
    *
    * @param schema
    * @return
    */
  def listTables(schema: Option[String]): Option[String] = schema match {
    case Some(schm) => Some(s"SELECT table_name  from all_tables where owner = '${schm}' ")
    case None       => Some("SELECT object_name FROM user_objects WHERE object_type = 'TABLE'")
  }

  /**
    *
    * @param table
    * @param condition
    * @return query that returns any ONE row from MsSQL table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val where = if (condition.isEmpty) " " else s" WHERE $condition "

    val from = schema match {
      case Some(x) => s" FROM $x.$table "
      case None    => s" FROM $table"
    }

    Some(s"SELECT *  $from $where AND ROWNUM = 1 ")
  }

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = {
    val errorCode = e.getErrorCode
    errorCode match {
      case ErrorCodes.INVALID_USER_OR_PASSWORD =>
        Map(s"${Oracle.K.User}|${Oracle.K.Password}" -> e.getMessage)
      case ErrorCodes.SID_NOT_RESOLVED =>
        Map(Oracle.K.Sid -> e.getMessage)
      case ErrorCodes.IO_ERROR =>
        Map(s"${Oracle.K.Host}|${Oracle.K.Port}" -> e.getMessage)
      case _ =>
        Exceptions.unknownException(e)
    }
  }

  private object ErrorCodes {
    // http://www.oracle.com/pls/db92/error_search
    final val INVALID_USER_OR_PASSWORD: Int = 1017 // ORA-01017
    final val SID_NOT_RESOLVED: Int         = 12505 // TNS-12505
    final val IO_ERROR: Int                 = 17002
  }

}

object Oracle extends DsnConfigCompanion {

  // Keys used in request
  private object K {
    final val User: String     = "user"
    final val Password: String = "password"
    final val Host: String     = "host"
    final val Port: String     = "port"
    final val Sid: String      = "sid"
    final val Service: String  = "service"
    final val Schema: String   = "schema"
  }

  def fromRequest(req: HttpServerRequest): Oracle = {

    val r = req.getParam _

    val user     = r(K.User)
    val password = r(K.Password)
    val host     = r(K.Host) orElse DataSource.Oracle.host
    val port     = r(K.Port).toInt.toOption.flatten orElse DataSource.Oracle.port
    val sid      = r(K.Sid)
    val service  = r(K.Service)
    val schema   = r(K.Schema)

    Oracle(user.get, password.get, host.get, port.get, sid, service, schema)
  }

}
