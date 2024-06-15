package edu.ucr.cs.cs167.srodr007

import edu.ucr.cs.bdlab.beast.geolite.{Feature, IFeature}
import org.apache.spark.SparkConf
import org.apache.spark.beast.SparkSQLRegistration
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import edu.ucr.cs.bdlab.beast._
import org.apache.spark.sql.functions.expr

import scala.collection.Map

/**
 * Scala examples for Beast
 */
object DataPreparation {
  def main(args: Array[String]): Unit = {
    // Initialize Spark context

    val conf = new SparkConf().setAppName("Part1")
    // Set Spark master to local if not already set
    if (!conf.contains("spark.master"))
      conf.setMaster("local[*]")

    val spark: SparkSession.Builder = SparkSession.builder().config(conf)

    val sparkSession: SparkSession = spark.getOrCreate()
    val sparkContext = sparkSession.sparkContext
    SparkSQLRegistration.registerUDT
    SparkSQLRegistration.registerUDF(sparkSession)

    val inputFile: String = args(0)
    try {
      val eBird: DataFrame = sparkSession.read.format("csv")
        .option("header", "true")
        //.option("delimiter", "\t")
        //.option("inferSchema", "true")
        .load(inputFile)

      val eBirdWithGeometry = eBird.withColumn("GEOMETRY", expr("ST_CreatePoint(x, y)"))

      val selectedColumns = Seq(
        "x",
        "y",
        "GLOBAL UNIQUE IDENTIFIER",
        "CATEGORY",
        "COMMON NAME",
        "SCIENTIFIC NAME",
        "OBSERVATION COUNT",
        "OBSERVATION DATE",
        "GEOMETRY"
      )

      val filteredDF = eBirdWithGeometry.select(selectedColumns.head, selectedColumns.tail: _*)

      val renamedDF = filteredDF.withColumnRenamed("GLOBAL UNIQUE IDENTIFIER", "GLOBAL_UNIQUE_IDENTIFIER")
        .withColumnRenamed("COMMON NAME", "COMMON_NAME")
        .withColumnRenamed("SCIENTIFIC NAME", "SCIENTIFIC_NAME")
        .withColumnRenamed("SUBSPECIES COMMON NAME", "SUBSPECIES_COMMON_NAME")
        .withColumnRenamed("OBSERVATION COUNT", "OBSERVATION_COUNT")
        .withColumnRenamed("OBSERVATION DATE", "OBSERVATION_DATE")

      val spatialRDD: SpatialRDD = renamedDF.toSpatialRDD

      val polygons: SpatialRDD = sparkContext.shapefile("tl_2018_us_zcta510.zip")

      val joinedRDD: RDD[(IFeature, IFeature)] = spatialRDD.spatialJoin(polygons)

      val resultDF: DataFrame = joinedRDD.map({ case (eBird, county) => Feature.append(eBird, county.getAs[String]("ZCTA5CE10"), "ZIPCode") })
        .toDataFrame(sparkSession)

      val finalDF = resultDF.drop("GEOMETRY")

      finalDF.write.mode(SaveMode.Overwrite).parquet("eBird_ZIP")
    }
    finally
    {
      sparkSession.stop()
    }
  }
}