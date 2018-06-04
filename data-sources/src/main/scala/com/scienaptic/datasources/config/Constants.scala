package com.scienaptic.datasources.config

object Constants {

  final val ServiceName: String = "data-sources"

  object Endpoints {

    /**
      * Get list of data sources supported
      */
    final val DataSources: String = "/data-sources"

    /**
      * Get list of databases for provided data source(:source)
      */
    final val DatabasesInDataSources: String = "/data-sources/:source/databases"

    /**
      * Get list of tables for provided data source(:source)
      */
    final val TablesInDataSources: String = "/data-sources/:source/tables"

    /**
      * Get list of columns and its metadata for provided data source(:source)
      */
    final val ColumnsWithMetadataInDataSources: String = "/data-sources/:source/columns"

    /**
      *  Get any one row from provided data source (database and table) which satisfies given ( `where` query param)  condition
      */
    final val RowFromTable: String = "/data-sources/:source/rows"

    /**
      *
      */
    final val SchemasInDatabase: String = "/data-sources/:source/schemas"
  }

}
