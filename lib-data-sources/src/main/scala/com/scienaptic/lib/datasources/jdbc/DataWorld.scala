package com.scienaptic.lib.datasources.jdbc

import java.net.URL
import java.sql.SQLException

import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import io.vertx.scala.core.http.HttpServerRequest

/**
  * Driver config for [[com.scienaptic.lib.datasources.DataSource.DataWorld]]
  *
  * @param user
  * @param token
  * @param dataset
  */
final case class DataWorld(user: String, token: String, dataset: String) extends DsnConfig {

  def driverClass: String = "world.data.jdbc.Driver"

  /**
    * Returns JDBC config for the data source
    */
  def getJdbcConfig: Option[JdbcConfig] =
    Some(JdbcConfig(driverClass, user, token, s"jdbc:data:world:sql:$user:$dataset"))

  def listTables(schema: Option[String]): Option[String] = Some(" ")

  /**
    *  query string for fetching listOfSchemas
    */
  val listSchemas = Some(s"  ")

  /**
    *
    * @param table
    * @param condition
    * @return  query that returns any ONE row from PostgreSQL table which matches where clause given by condition
    */
  def getRow(table: String, condition: String): Option[String] = {

    val where = if (condition.isEmpty) " " else s" WHERE $condition "

    Some(s"SELECT * $table $where LIMIT 1 ")
  }

  /**
    *
    * @param table
    * @return
    */
  def getColumnsMetadataConfig(table: String): MetadataConfig = MetadataConfig(user, dataset, table, "%")

  /**
    * Errors on a database access
    */
  def getSQLExceptionErrors(e: SQLException): ErrorMap = {
    val message = e.getMessage.split(";").head
    val pattern = ".*failed with response (\\d{3}).*".r
    message match {
      case pattern(code) =>
        code match {
          case "401" =>
            Map(DataWorld.K.Token -> s"Invalid token used for dataset url https://data.world/$user/$dataset")
          case "404" =>
            Map(DataWorld.K.DatasetUrl -> s"Dataset url https://data.world/$user/$dataset not found.")
          case _ => Map(Exceptions.Form_Error -> e.getMessage)
        }
      case _ => Map(Exceptions.Form_Error -> e.getMessage)
    }
  }

}

object DataWorld extends DsnConfigCompanion {

  // Keys used in request
  private object K {
    // Dsn
    final val DatasetUrl: String = "datasetUrl"
    final val Token: String      = "token"

    // Metadata
    final val Table: String = "table"
  }

  case class UserDataset(user: String, dataset: String)

  private def parseUrl(datasetUrl: String): UserDataset = {
    val urlParts = new URL(datasetUrl).getPath.split("/")
    urlParts.length match {
      case x if x >= 3 => UserDataset(urlParts(1), urlParts(2))
      case 2           => UserDataset(urlParts(1), "")
      case _           => UserDataset("", "")
    }
  }

  def fromRequest(req: HttpServerRequest): DataWorld = {

    val r = req.getParam _

    val datasetUrl = r(K.DatasetUrl)
    val token      = r(K.Token)

    val userDataset = parseUrl(datasetUrl.get)

    DataWorld(userDataset.user, token.get, userDataset.dataset)
  }

}
