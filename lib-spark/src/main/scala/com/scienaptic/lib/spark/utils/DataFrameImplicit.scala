package com.scienaptic.lib.spark.utils

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.NumericType

object DataFrameImplicit {

  implicit class DataFrameHelper(val dataFrame: DataFrame) extends AnyVal {

    /**
      * Get Numeric Columns from DataFrame.
      *
      * @return array of numeric column names.
      */
    def numericColumns: Array[String] = dataFrame.schema.fields.filter(_.dataType.isInstanceOf[NumericType]).map(_.name)

    /**
      * Select specified columns from DataFrame.
      *
      * @param cols array of column names to be selected from DataFrame.
      * @return DataFrame with specified columns.
      */
    def select(cols: Array[String]): DataFrame = dataFrame.select(cols.head, cols.tail: _*)

  }

}
