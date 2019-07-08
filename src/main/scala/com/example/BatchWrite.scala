package com.example

import org.apache.spark.sql.SparkSession

object BatchWrite {

  def main(args : Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("data-sink")
      .config("spark.cores.max", 2)
      .getOrCreate()

    import spark.implicits._
    spark.createDataset(1 to 10)
      .write
      .format("pulsar")
      .option("service.url", "pulsar://localhost:6650")
      .option("admin.url", "http://localhost:8088")
      .option("topic", "topic-test")
      .save()

  }
}
