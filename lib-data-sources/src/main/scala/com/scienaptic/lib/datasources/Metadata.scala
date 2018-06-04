package com.scienaptic.lib.datasources

import java.sql._

import com.scienaptic.lib.datasources.jdbc.{ DsnConfig, MetadataConfig }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Metadata {

  case class ForeignKey(fkSequence: String, pkTableCatalog: String, pkTableName: String, pkColumnName: String)

  case class Columns(columnName: String,
                     nativeType: String,
                     columnSize: Int,
                     isPrimaryKey: Boolean,
                     foreignKey: Option[ForeignKey],
                     isNullable: String,
                     isAutoIncrement: String,
                     defaultValue: String,
                     remarks: String,
                     jdbcType: Int,
                     irisType: Option[String],
                     ordinalPosition: Int)

  /**
    * Connect to data source and return meta information of columns in the provided table.
    * Errors are handled by [[DsnConfig.getConnectionErrors()]]
    *
    * @param dsnConfig DSN Data to connect to a data source
    * @param table     Table name to retrieve the metadata.
    * @return
    */
  def getColumnsFuture(dsnConfig: DsnConfig, table: String): Future[Option[Seq[Columns]]] =
    Future {
      for {
        jdbcConfig <- dsnConfig.getJdbcConfig
      } yield {
        Class.forName(jdbcConfig.driverClass)
        val connection = DriverManager.getConnection(jdbcConfig.url, jdbcConfig.user, jdbcConfig.password)
        val columns    = getColumns(connection.getMetaData, dsnConfig.getColumnsMetadataConfig(table))
        connection.close()
        columns
      }
    }

  /**
    * Get primary key columns
    *
    * @param metadata Comprehensive information about the database as a whole.
    * @param config   Catalog, Schema, Table and Column Pattern
    * @return
    */
  private def getPrimaryKeys(metadata: DatabaseMetaData, config: MetadataConfig): Seq[String] = {

    val primaryKeys = metadata.getPrimaryKeys(config.catalog, config.schemaPattern, config.tableNamePattern)

    new Iterator[String] {

      override def hasNext: Boolean = primaryKeys.next

      override def next: String = primaryKeys.getString("COLUMN_NAME")

    }.toList
  }

  /**
    * Get foreign key columns with foreign keys.
    *
    * @param metadata Comprehensive information about the database as a whole.
    * @param config   Catalog, Schema, Table and Column Pattern
    * @return
    */
  private def getForeignKeys(metadata: DatabaseMetaData, config: MetadataConfig): Map[String, ForeignKey] = {

    val importedKeys = metadata.getImportedKeys(config.catalog, config.schemaPattern, config.tableNamePattern)

    new Iterator[(String, ForeignKey)] {

      override def hasNext: Boolean = importedKeys.next

      override def next: (String, ForeignKey) = importedKeys.getString("FKCOLUMN_NAME") -> ForeignKey(
        fkSequence = importedKeys.getString("KEY_SEQ"),
        pkTableCatalog = importedKeys.getString("PKTABLE_CAT"),
        pkTableName = importedKeys.getString("PKTABLE_NAME"),
        pkColumnName = importedKeys.getString("PKCOLUMN_NAME")
      )
    }.map(x => x).toMap
  }

  /**
    * Get columns with meta information of each one.
    *
    * @param metadata Comprehensive information about the database as a whole.
    * @param config   Catalog, Schema, Table and Column Pattern
    * @return
    */
  private def getColumns(metadata: DatabaseMetaData, config: MetadataConfig): Seq[Columns] = {

    val primaryKeys = getPrimaryKeys(metadata, config)
    val foreignKeys = getForeignKeys(metadata, config)

    val columns =
      metadata.getColumns(config.catalog, config.schemaPattern, config.tableNamePattern, config.columnNamePattern)

    new Iterator[Columns] {

      override def hasNext: Boolean = columns.next

      override def next: Columns = {
        val name     = columns.getString("COLUMN_NAME")
        val jdbcType = columns.getInt("DATA_TYPE")

        Columns(
          columnName = name,
          nativeType = columns.getString("TYPE_NAME"),
          columnSize = columns.getInt("COLUMN_SIZE"),
          isPrimaryKey = primaryKeys.contains(name),
          foreignKey = foreignKeys.get(name),
          isNullable = columns.getString("IS_NULLABLE"),
          isAutoIncrement = columns.getString("IS_AUTOINCREMENT"),
          defaultValue = columns.getString("COLUMN_DEF"),
          remarks = columns.getString("REMARKS"),
          jdbcType = jdbcType,
          ordinalPosition = columns.getInt("ORDINAL_POSITION"),
          irisType = jdbcTypeToIrisType(jdbcType)
        )
      }
    }.toList

  }

  object jdbcTypeToIrisType {
    import java.sql.Types._

    val jdbcTypeToIrisMap = Map(
      TINYINT      -> "NUMBER",
      DECIMAL      -> "NUMBER",
      INTEGER      -> "NUMBER",
      SMALLINT     -> "NUMBER",
      BIGINT       -> "NUMBER",
      FLOAT        -> "NUMBER",
      DOUBLE       -> "NUMBER",
      NUMERIC      -> "NUMBER",
      DECIMAL      -> "NUMBER",
      REAL         -> "NUMBER",
      CHAR         -> "STRING",
      VARCHAR      -> "STRING",
      LONGVARCHAR  -> "STRING",
      NCHAR        -> "STRING",
      NVARCHAR     -> "STRING",
      LONGNVARCHAR -> "STRING",
      DATE         -> "STRING",
      TIME         -> "STRING",
      TIMESTAMP    -> "STRING",
      BIT          -> "BOOLEAN"
    )

    def apply(i: Int): Option[String] =
      jdbcTypeToIrisMap.get(i)

  }

}
