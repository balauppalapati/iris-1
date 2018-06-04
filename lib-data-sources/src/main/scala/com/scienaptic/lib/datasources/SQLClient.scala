package com.scienaptic.lib.datasources

import com.scienaptic.lib.common.annotations.impure
import io.vertx.core.json.{ JsonArray, JsonObject }
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.jdbc.JDBCClient
import io.vertx.scala.ext.sql.ResultSet

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Future

/**
  *
  * @param vertx  the deployed Vert.x instance
  * @param config jdbc driver config
  */
final class SQLClient(vertx: Vertx, config: JsonObject) {

  private implicit val executionContext: VertxExecutionContext =
    VertxExecutionContext(vertx.getOrCreateContext)

  private val client = JDBCClient.createNonShared(vertx, config)

  /**
    * Errors are handled by [[com.scienaptic.lib.datasources.jdbc.DsnConfig.getConnectionErrors()]]
    *
    * @param sql the SQL to execute. For example <code>SELECT * FROM table ...</code>.
    * @return query results
    */
  @impure def queryFuture(sql: String): Future[ResultSet] =
    for {
      connection <- client.getConnectionFuture
      result     <- connection.queryFuture(sql)
    } yield result

  /**
    * Get any ONE row from given data source matching where condition in the sql (param) query string
    *
    * @param sql  query on table with where condition which returns any ONE row that matches
    * @return     a Future[JsonArray]
    */

  @impure def queryRow(sql: String): Future[JsonArray] =
    for {
      connection <- client.getConnectionFuture
      result     <- connection.queryFuture(sql)
    } yield {
      SQLClient.getColumn(result.getResults)
    }

  /**
    * Get a flattened column for sql queries returning a single column.
    * Errors are handled by [[com.scienaptic.lib.datasources.jdbc.DsnConfig.getConnectionErrors()]]
    *
    * @note Only pass sql queries which returns single column. Passing other queries flattens multiple columns.
    * @param sql the SQL to execute. For example <code>SELECT singleCol FROM table ...</code>.
    * @return query results for a single column
    */
  @impure def queryColumnFuture(sql: String): Future[JsonArray] =
    for {
      connection <- client.getConnectionFuture
      result     <- connection.queryFuture(sql)
    } yield {
      SQLClient.getColumn(result.getResults)
    }

}

object SQLClient {

  /**
    * Use to flatten single column sql results into an array
    * [ ["X"], ["Y"] ] ==> ["X", "Y"]
    *
    * @param results Single column sql result
    * @return flattened array
    */
  private def getColumn(results: mutable.Buffer[JsonArray]): JsonArray =
    new JsonArray(results.flatMap(_.getList.toArray).asJava)

}
