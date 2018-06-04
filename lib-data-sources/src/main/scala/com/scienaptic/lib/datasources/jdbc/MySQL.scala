package com.scienaptic.lib.datasources.jdbc

import java.sql.{ SQLException, SQLNonTransientConnectionException }

import com.mysql.cj.core.exceptions.{ CJCommunicationsException, MysqlErrorNumbers }
import com.mysql.cj.jdbc.exceptions.CommunicationsException
import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.Equal._
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.DataSource
import io.vertx.scala.core.http.HttpServerRequest

import scala.util.control.NonFatal

/**
  * Driver config for
  * <ol>
  * <li>[[com.scienaptic.lib.datasources.DataSource.MySQL]]</li>
  * <li>[[com.scienaptic.lib.datasources.DataSource.MariaDB]]</li>
  * <li>[[com.scienaptic.lib.datasources.DataSource.MemSQL]]</li>
  * </ol>
  *
  * @param user
  * @param password
  * @param host
  * @param port
  * @param database
  * @param ssl
  */
final case class MySQL(user: String,
                       password: String,
                       host: String,
                       port: Int,
                       database: String,
                       ssl: Boolean,
                       connectTimeoutMilliseconds: Int,
                       socketTimeoutMilliseconds: Int)
    extends DsnConfig {

  def driverClass: String = "com.mysql.cj.jdbc.Driver"

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    Some(
      JdbcConfig(
        driverClass,
        user,
        password,
        s"jdbc:mysql://$host:$port/$database?useSSL=$ssl&connectTimeout=$connectTimeoutMilliseconds&socketTimeout=$connectTimeoutMilliseconds"
      )
    )

  /**
    * Server instance == Not identified with catalog, just a set of databases.
    * Database == Schema == Catalog == A namespace within the server.
    * User == Named account, who can connect to server and use (but can not own - no concept of ownership)
    * objects in one or more databases
    * To identify any object in running server, you need (database name + object name)
    *
    * @param table
    * @return
    */
  def getColumnsMetadataConfig(table: String): MetadataConfig = MetadataConfig(database, "", table, "%")

  /**
    *
    * @param schema
    * @return
    */
  def listTables(schema: Option[String]): Option[String] = Some("SHOW TABLES")

  val listSchemas = Some("SHOW DATABASES")

  /**
    *
    * @param table
    * @param condition
    * @return query that returns any ONE row from MySQL table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val whereClause = if (condition.isEmpty) " " else s" WHERE $condition "

    Some(s"SELECT * FROM $table $whereClause LIMIT 1 ")
  }

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = {
    val errorCode = e.getErrorCode
    if (errorCode =/= 0) {
      errorCode match {
        // https://dev.mysql.com/doc/refman/8.0/en/error-messages-server.html
        case MysqlErrorNumbers.ER_ACCESS_DENIED_ERROR =>
          Map(s"${MySQL.K.User}|${MySQL.K.Password}" -> e.getMessage)
        case MysqlErrorNumbers.ER_BAD_DB_ERROR | MysqlErrorNumbers.ER_WRONG_DB_NAME =>
          Map(MySQL.K.Database -> e.getMessage)
        case MysqlErrorNumbers.ER_NO_SUCH_TABLE | MysqlErrorNumbers.ER_WRONG_TABLE_NAME =>
          Map(MySQL.K.Table -> e.getMessage)
        case _ => Exceptions.unknownException(e)
      }
    } else {
      getConnectionErrorsNoVendorCode(e)
    }
  }

  /**
    * Errors on a database connection
    */
  private def getConnectionErrorsNoVendorCode(e: SQLException): ErrorMap = e match {
    case x: CommunicationsException => getCommunicationErrors(x)
    case _: SQLNonTransientConnectionException =>
      Exceptions.getCause(e) match {
        case Some(x: IllegalArgumentException) => Map(MySQL.K.Port -> x.getMessage)
        case Some(NonFatal(x))                 => Exceptions.unknownException(x)
        case None                              => Exceptions.unknownException(e)
      }
    case NonFatal(_) => Exceptions.unknownException(e)
  }

  /**
    * Errors on a communicating with database.
    */
  private def getCommunicationErrors(e: CommunicationsException): ErrorMap =
    Exceptions.getCause(e) match {
      case Some(x: CJCommunicationsException) =>
        Exceptions.getCause(x) match {
          case Some(_: java.net.UnknownHostException) => Map(MySQL.K.Host -> s"Unknown host: $host")
          case Some(y: java.net.ConnectException)     => Map(s"${MySQL.K.Host}|${MySQL.K.Port}" -> y.getMessage)
          case Some(NonFatal(y))                      => Exceptions.unknownException(y)
          case None                                   => Exceptions.unknownException(e)
        }
      case Some(NonFatal(x)) => Exceptions.unknownException(x)
      case None              => Exceptions.unknownException(e)
    }
}

object MySQL extends DsnConfigCompanion {

  final val CONNECT_TIMEOUT_IN_MILLISECONDS = 0
  final val SOCKET_TIMEOUT_IN_MILLISECONDS  = 0

  // Keys used in request
  private object K {
    // Dsn
    final val User: String           = "user"
    final val Password: String       = "password"
    final val Host: String           = "host"
    final val Port: String           = "port"
    final val Database: String       = "database"
    final val Ssl: String            = "ssl"
    final val ConnectTimeout: String = "connectTimeout"
    final val SocketTimeout: String  = "socketTimeout"

    // Metadata
    final val Table: String = "table"
  }

  def fromRequest(req: HttpServerRequest): MySQL = {

    val r = req.getParam _

    val user           = r(K.User)
    val password       = r(K.Password)
    val host           = r(K.Host) orElse DataSource.MySQL.host
    val port           = r(K.Port).toInt.toOption.flatten orElse DataSource.MySQL.port
    val database       = r(K.Database) orElse DataSource.MySQL.database
    val ssl            = r(K.Ssl).toBoolean.toOption.flatten orElse DataSource.MySQL.ssl
    val connectTimeout = r(K.ConnectTimeout).toInt.toOption.flatten getOrElse CONNECT_TIMEOUT_IN_MILLISECONDS
    val socketTimeout  = r(K.SocketTimeout).toInt.toOption.flatten getOrElse SOCKET_TIMEOUT_IN_MILLISECONDS

    MySQL(user.get, password.get, host.get, port.get, database.get, ssl.get, connectTimeout, socketTimeout)
  }

}
