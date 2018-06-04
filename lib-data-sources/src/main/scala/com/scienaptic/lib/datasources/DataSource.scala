package com.scienaptic.lib.datasources

import com.scienaptic.lib.datasources.interfaces.DsnConfigParamsType
import com.scienaptic.lib.datasources.jdbc.{ DsnConfig, DsnConfigCompanion }
import enumeratum.{ Enum, EnumEntry }

import scala.collection.immutable

sealed trait DataSource extends EnumEntry

/** Enum for DataSources */
object DataSource extends Enum[DataSource] {

  val values: immutable.IndexedSeq[DataSource] = findValues

  case object DataWorld extends DataSource

  case object DB2 extends DataSource

  case object MariaDB extends DataSource

  case object MemSQL extends DataSource

  case object MsSQL extends DataSource

  case object MySQL extends DataSource

  case object Oracle extends DataSource

  case object PostgreSQL extends DataSource

  case object Redshift extends DataSource

  /** Default ports for various data sources. */
  private object DefaultPorts {
    final val DB2: Int        = 50000
    final val MsSQL: Int      = 1433
    final val Mysql: Int      = 3306
    final val Oracle: Int     = 1521
    final val PostgreSQL: Int = 5432
    final val Redshift: Int   = 5439
  }

  implicit class DataSourceHelper(dataSource: DataSource) {

    /** Is dataSource MySQL based. */
    def isMySQLBased: Boolean = dataSource match {
      case MySQL | MariaDB | MemSQL => true
      case _                        => false
    }

    /** Default host for dataSource */
    def host: Option[String] = Some("localhost")

    /** Default port for dataSource */
    def port: Option[Int] = dataSource match {
      case _ if isMySQLBased => Some(DefaultPorts.Mysql)
      case DB2               => Some(DefaultPorts.DB2)
      case MsSQL             => Some(DefaultPorts.MsSQL)
      case Oracle            => Some(DefaultPorts.Oracle)
      case PostgreSQL        => Some(DefaultPorts.PostgreSQL)
      case Redshift          => Some(DefaultPorts.Redshift)
    }

    /** Default database for dataSource */
    def database: Option[String] = dataSource match {
      case _ if isMySQLBased             => Some("")
      case MsSQL                         => Some("master")
      case PostgreSQL                    => Some("postgres")
      case Oracle | Redshift | DataWorld => None
    }

    /** Default ssl for dataSource */
    def ssl: Option[Boolean] = dataSource match {
      case Redshift => Some(true)
      case _        => Some(false)
    }

    /** Show databases query to fetch all databases of a connection. */
    def listDatabases: Option[String] = dataSource match {
      case _ if isMySQLBased        => Some("SHOW DATABASES")
      case MsSQL                    => Some("SELECT name FROM sys.databases")
      case PostgreSQL | Redshift    => Some("SELECT datname FROM pg_database WHERE datistemplate = false")
      case Oracle | DB2 | DataWorld => None
    }

    /** Does data source has a JDBC driver */
    def isJDBCDriver: Boolean = dataSource match {
      case _ if isMySQLBased                            => true
      case DB2 | MsSQL | Oracle | PostgreSQL | Redshift => true
    }

    /** Get Dsn Config Companion of data source */
    def dsnConfigCompanion: Option[DsnConfigCompanion] = dataSource match {
      case _ if isMySQLBased => Some(jdbc.MySQL)
      case DataWorld         => Some(jdbc.DataWorld)
      case DB2               => Some(jdbc.DB2)
      case MsSQL             => Some(jdbc.MsSQL)
      case Oracle            => Some(jdbc.Oracle)
      case PostgreSQL        => Some(jdbc.PostgreSQL)
      case Redshift          => Some(jdbc.Redshift)
    }

    /** Get Dsn Config of data source */
    def dsnConfig(params: DsnConfigParamsType): Option[DsnConfig] =
      dataSource.dsnConfigCompanion match {
        case Some(obj) => Some(obj(params))
        case _         => None
      }
  }

}
