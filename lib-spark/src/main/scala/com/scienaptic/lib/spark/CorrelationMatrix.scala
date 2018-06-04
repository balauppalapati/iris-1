package com.scienaptic.lib.spark

import com.scienaptic.lib.spark.Common.CorrelationMatrix.{ Request, Response }
import com.scienaptic.lib.spark.Common.CsvAttributes
import com.scienaptic.lib.spark.utils.DataFrameImplicit.DataFrameHelper
import com.scienaptic.lib.spark.utils.{ ArrayUtils, Rounding }
import mist.api._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.sql.{ DataFrame, SparkSession }

object CorrelationMatrix extends MistFn[Response] {

  private def readCsv(sparkSession: SparkSession, filePath: String, attributes: CsvAttributes): DataFrame =
    sparkSession.read
      .option("inferSchema", "true")
      .option("header", "true")
      .option("delimiter", attributes.delimiter)
      .option("quote", attributes.quote)
      .option("escape", attributes.escape)
      .csv(filePath)

  def handle: Handle[Response] =
    withArgs(arg[Request]("request")).withMistExtras
      .onSparkSession((request: Request, extras: MistExtras, sparkSession: SparkSession) => {

        import extras._
        logger.info(s"Job Id - $jobId")

        val dataFrame = readCsv(sparkSession, request.filePath, request.attributes)

        // Select numeric columns.
        val numericColumns = dataFrame.numericColumns
        // Select requested columns from numeric columns.
        val selectedColumns = ArrayUtils.intersect(request.columns, numericColumns)
        // Get DataFrame with only selected columns.
        val numericDataFrame = dataFrame.select(selectedColumns)

        // Create Vector Assembler object
        val assembler = new VectorAssembler().setInputCols(selectedColumns).setOutputCol("features")
        // Select Features and convert to rdd.
        val rddRows = assembler.transform(numericDataFrame).select("features").rdd

        // Extract Vectors from Row
        val correlationInput = rddRows
          .map(_.getAs[org.apache.spark.ml.linalg.Vector](0))
          .map(org.apache.spark.mllib.linalg.Vectors.fromML)

        // Compute the correlation matrix for the input RDD of Vectors using the specified method
        val correlationMatrix = Statistics.corr(correlationInput, request.method)
        // Round off correlation matrix to specified precision.
        val correlationArray = request.precision match {
          case Some(scale) => correlationMatrix.toArray.map(Rounding.toDouble(_, scale))
          case None        => correlationMatrix.toArray
        }

        Response(selectedColumns, correlationArray.grouped(selectedColumns.length).toArray)
      })
}
