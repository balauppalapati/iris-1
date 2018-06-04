package com.scienaptic.datasources

import com.scienaptic.lib.datasources.DataSource
import com.scienaptic.lib.datasources.DataSource._
import com.scienaptic.lib.filesources.FileSource
import com.scienaptic.lib.filesources.FileSource._

object Sources {

  case class Source(k: String, v: String)

  case class SourceWithAttributes(title: String, sources: Seq[Source])

  case class SourcesResponse(file: SourceWithAttributes, database: SourceWithAttributes)

  def sources: SourcesResponse = SourcesResponse(
    file = SourceWithAttributes(
      title = "File",
      sources = FileSource.values.map(x => Source(x.entryName.toLowerCase, x.text))
    ),
    database = SourceWithAttributes(
      title = "Database",
      DataSource.values.map(x => Source(x.entryName.toLowerCase, x.text))
    )
  )

  private implicit class DataSourceHelper(dataSource: DataSource) {

    def text: String = dataSource match {
      case DataWorld  => "Data.World"
      case DB2        => "IBM DB2"
      case MariaDB    => "MariaDB"
      case MemSQL     => "MemSQL"
      case MsSQL      => "Microsoft SQL Server"
      case MySQL      => "MySQL"
      case Oracle     => "Oracle"
      case PostgreSQL => "PostgreSQL"
      case Redshift   => "Amazon Redshift"
    }
  }

  private implicit class FileSourceHelper(fileSource: FileSource) {

    def text: String = fileSource match {
      case CSV   => "Delimited Text Files"
      case Excel => "Excel Files"
    }
  }

}
