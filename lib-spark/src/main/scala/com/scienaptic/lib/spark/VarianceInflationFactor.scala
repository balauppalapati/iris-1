package com.scienaptic.lib.spark

import com.scienaptic.lib.spark.Common.CsvAttributes
import com.scienaptic.lib.spark.Common.VarianceInflationFactor.{ Request, Response }
import com.scienaptic.lib.spark.utils.DataFrameImplicit.DataFrameHelper
import com.scienaptic.lib.spark.utils.{ ArrayUtils, Rounding }
import mist.api._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.sql.{ DataFrame, SparkSession }

object VarianceInflationFactor extends MistFn[Response] {

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

        val vifValues = numericDataFrame.columns.map(col => {
          // All attributes except the one coming from the loop are independent.
          val inputColumns = selectedColumns.filter(_ != col)
          // Create Vector Assembler object
          val assembler = new VectorAssembler().setInputCols(inputColumns).setOutputCol("features")
          // Merge the VectorAssembler object and all attributes except the loop order into a single column
          val assembledDataFrame = assembler.transform(numericDataFrame)
          // Create a linear regression object set input qualities and target qualities.
          val linearRegression = new LinearRegression().setFeaturesCol("features").setLabelCol(col)
          // Train the linear linearRegressionModel you created
          val linearRegressionModel = linearRegression.fit(assembledDataFrame)
          // Trained linearRegressionModel r2 calculated. In this mode, calculate VIF using r2.
          val vif = 1 / (1 - linearRegressionModel.summary.r2)
          // Round VIF
          request.precision match {
            case Some(scale) => Rounding.toDouble(vif, scale)
            case None        => vif
          }
        })

        Response(selectedColumns, vifValues)
      })
}
