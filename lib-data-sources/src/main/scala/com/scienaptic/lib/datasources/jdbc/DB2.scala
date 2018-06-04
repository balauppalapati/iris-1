package com.scienaptic.lib.datasources.jdbc

import java.sql.SQLException

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import com.scienaptic.lib.common.util.OptionStringImplicit.OptionStringHelper
import com.scienaptic.lib.datasources.DataSource
import io.vertx.scala.core.http.HttpServerRequest

import scala.util.control.NonFatal

/**
  * Driver config for [[com.scienaptic.lib.datasources.DataSource.DB2]]
  *
  * @param user
  * @param password
  * @param host
  * @param port
  * @param database
  * @param ssl
  */
final case class DB2(user: String,
                     password: String,
                     host: String,
                     port: Int,
                     database: String,
                     ssl: Boolean,
                     schema: Option[String])
    extends DsnConfig {

  def driverClass: String = "com.ibm.db2.jcc.DB2Driver"

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    Some(JdbcConfig(driverClass, user, password, s"jdbc:db2://$host:$port/$database"))

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
  def getColumnsMetadataConfig(table: String): MetadataConfig =
    MetadataConfig(null, null, table, "%") // scalastyle:ignore

  /**
    *
    * @param schema
    * @return
    */
  def listTables(schema: Option[String]): Option[String] = schema match {
    case Some(x) => Some(s"SELECT name FROM sysibm.systables WHERE type = 'T' AND creator = '$x'")
    case None =>
      val systemSchemas    = Seq("SYSCAT", "SYSIBM", "SYSIBMADM", "SYSPUBLIC", "SYSSTAT", "SYSTOOLS")
      val creatorCondition = systemSchemas.map(t => s"creator != '$t'").mkString(" AND ")
      Some(s"SELECT name FROM sysibm.systables WHERE type = 'T' AND $creatorCondition")
  }

  /**
    * @param table
    * @param condition
    * @return query that returns any ONE row from DB2 table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val where = if (condition.isEmpty) " " else s" WHERE $condition "

    val from = schema match {
      case Some(x) => s" FROM $x.$table "
      case None    => s" FROM $table"
    }

    Some(s"SELECT * $from $where FETCH FIRST 1 ROWS ONLY")
  }

  /**
    * query string for fetching listOfSchemas
    */
  val listSchemas = Some("SELECT TABSCHEMA FROM syscat.tables")

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = e.getSQLState match {
    // https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/codes/src/tpc/db2z_sqlstatevalues.html
    case "28000" =>
      Map(s"${DB2.K.User}|${DB2.K.Password}" -> e.getMessage)
    case "08001" =>
      Exceptions.getCause(e) match {
        case Some(_: java.net.ConnectException) =>
          Map(DB2.K.Port -> e.getMessage)
        case Some(NonFatal(_)) =>
          Map(s"${DB2.K.Host}|${DB2.K.Port}" -> e.getMessage)
        case None =>
          Map(DB2.K.Host -> e.getMessage)
      }
    case "08004" =>
      Map(DB2.K.Database -> e.getMessage)
    case _ => Exceptions.unknownException(e)
  }

}

object DB2 extends DsnConfigCompanion {

  // Keys used in request
  private object K {
    // Dsn
    final val User: String     = "user"
    final val Password: String = "password"
    final val Host: String     = "host"
    final val Port: String     = "port"
    final val Database: String = "database"
    final val Ssl: String      = "ssl"
    final val Schema: String   = "schema"

    // Metadata
    final val Table: String = "table"
  }

  def fromRequest(req: HttpServerRequest): DB2 = {

    val r = req.getParam _

    val user     = r(K.User)
    val password = r(K.Password)
    val host     = r(K.Host) orElse DataSource.DB2.host
    val port     = r(K.Port).toInt.toOption.flatten orElse DataSource.DB2.port
    val database = r(K.Database)
    val ssl      = r(K.Ssl).toBoolean.toOption.flatten orElse DataSource.DB2.ssl
    val schema   = r(K.Schema)

    DB2(user.get, password.get, host.get, port.get, database.get, ssl.get, schema)
  }

}
