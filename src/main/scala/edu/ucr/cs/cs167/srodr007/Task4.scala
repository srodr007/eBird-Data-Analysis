package edu.ucr.cs.cs167.srodr007

import org.apache.spark.SparkConf
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, StringIndexer, Tokenizer}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.sql.functions._

object Task4 {

  def main(args: Array[String]): Unit = {
    //    if (args.length != 1) {
    //      println("Usage: <input file>")
    //      println("  - <input file> path to a Parquet file input")
    //      sys.exit(0)
    //    }
    //    val inputFilePath = args(0)
    val conf = new SparkConf()
      .setAppName("CS167 Project B Part 4")
      .setMaster("local[*]")
    println(s"Using Spark master '${conf.get("spark.master")}'")
    val spark = SparkSession.builder()
      .config(conf)
      .getOrCreate()

    val t1 = System.nanoTime
    try {
      // Load the dataset
      //val birdSightings: DataFrame = spark.read.parquet(inputFilePath)
      val birdSightings: DataFrame = spark.read.parquet("eBird_ZIP/*.parquet")

      birdSightings.createOrReplaceTempView("bird_sightings")

      // Filter records with CATEGORY values species, form, issf, or slash using Spark SQL
      val filteredBirdSightings = spark.sql("SELECT * FROM bird_sightings WHERE CATEGORY IN ('species', 'form', 'issf', 'slash')")

      // Concatenate common name and scientific name into a single column
      val concatenatedData = filteredBirdSightings.withColumn("text", concat(col("COMMON_NAME"), lit(" "), col("SCIENTIFIC_NAME")))

      val tokenizer = new Tokenizer()
        .setInputCol("text")
        .setOutputCol("tokens")

      val hashingTF = new HashingTF()
        .setInputCol(tokenizer.getOutputCol)
        .setOutputCol("features")

      val categoryIndexer = new StringIndexer()
        .setInputCol("CATEGORY")
        .setOutputCol("label")

      // Define the logistic regression classifier
      val classifier = new LogisticRegression()
        .setFeaturesCol("features")
        .setLabelCol("label")

      // Define the pipeline
      val pipeline = new Pipeline()
        .setStages(Array(tokenizer, hashingTF, categoryIndexer, classifier))

      // Split data into training and testing sets using 80/20 split
      val Array(trainingData, testData) = concatenatedData.randomSplit(Array(0.8, 0.2))

      // Train the model
      val model = pipeline.fit(trainingData)

      // Make predictions on the test data
      val predictions = model.transform(testData)

      val t2 = System.nanoTime
      val totalTimeSeconds = (t2 - t1) * 1E-9

      // Evaluate the model
      val MulticlassClassificationEvaluator = new MulticlassClassificationEvaluator()
        .setLabelCol("label")
        .setPredictionCol("prediction")

      val accuracy = MulticlassClassificationEvaluator.setMetricName("accuracy").evaluate(predictions)
      val precision = MulticlassClassificationEvaluator.setMetricName("weightedPrecision").evaluate(predictions)
      val recall = MulticlassClassificationEvaluator.setMetricName("weightedRecall").evaluate(predictions)

      // Display sample predictions
      predictions.select("COMMON_NAME", "SCIENTIFIC_NAME", "CATEGORY", "label", "prediction").show()

      // Print evaluation metrics
      println(s"Total Time: $totalTimeSeconds seconds")
      println(s"Accuracy: $accuracy")
      println(s"Precision: $precision")
      println(s"Recall: $recall")

    } finally {
      spark.stop()
    }
  }
}