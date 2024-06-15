package edu.ucr.cs.cs167.srodr007

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.SparkConf


/**
 * Scala examples for Beast
 */

object TemporalAnalysis {
  def main(args: Array[String]): Unit = {

    // Raise error in case the date arguments are not well introduced
    if (args.length != 2 || args(0).length != 10 || args(1).length != 10 ||
        args(0).charAt(2) != '/' || args(0).charAt(5) != '/' ||
        args(1).charAt(2) != '/' || args(1).charAt(5) != '/' ) {
      println("Usage: App MM/dd/yyyy MM/dd/yyyy")
      System.exit(1)
    }

    // New Spark configuration
    val conf = new SparkConf()
      .setAppName("Project B Task 3: Temporal Analysis")

    // Set Spark master to local
    if (!conf.contains("spark.master")) {
      conf.setMaster("local[*]")
    }
    println(s"Using Spark master '${conf.get("spark.master")}'")

    // Creating the SparkSession
    val spark: SparkSession = SparkSession
      .builder()
      .config(conf)
      .getOrCreate()

    // Extracting the start and end dates using the arguments
    val startDate = args(0)
    val endDate = args(1)

    try {
      // Creating the DataFrame with the parquet file
      val df: DataFrame = spark.read.parquet("eBird_ZIP/*.parquet")

      // Register the DataFrame as a SQL temporary view
      df.createOrReplaceTempView("observations")

      // Query to select observations between the start and end dates
      val query = s"""
        SELECT COMMON_NAME, SUM(OBSERVATION_COUNT) as num_observations
        FROM observations
        WHERE to_date(OBSERVATION_DATE, 'yyyy-MM-dd') BETWEEN to_date('$startDate', 'MM/dd/yyyy') AND to_date('$endDate', 'MM/dd/yyyy')
        GROUP BY COMMON_NAME
      """

      // Save results
      val resultDf = spark.sql(query)

      // In case there is a null value for certain COMMON_NAME, write a 0 instead of nothing.
      val ResultDf = resultDf.na.fill(0, Seq("num_observations"))

      // Writing the result to a CSV file
      ResultDf.write
        .format("csv")
        .option("header", "true")
        .option("delimiter", ";") // Use semicolon as the delimiter because Excel uses it to separate between columns
        .save("eBirdObservationsTime.csv")
    } finally {
      spark.stop()
    }
  }
}
