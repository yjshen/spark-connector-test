package com.example

import org.apache.spark.sql.SparkSession

object StreamRead {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("data-read")
      .config("spark.cores.max", 2)
      .getOrCreate()

    val ds = spark.readStream
      .format("pulsar")
      .option("service.url", "pulsar://localhost:6650")
      .option("admin.url", "http://localhost:8088")
      .option("topic", "topic-test")
      .load()

    ds.printSchema()

    val query = ds.writeStream
      .outputMode("append")
      .format("console")
      .start()

    query.awaitTermination()
  }

}
