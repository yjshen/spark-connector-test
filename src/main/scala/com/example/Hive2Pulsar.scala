package com.example

import org.apache.spark.sql.SparkSession

object Hive2Pulsar {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("Hive to Pulsar Transfer")
      .config("spark.cores.max", 2)
      .enableHiveSupport()
      .getOrCreate()

    val hiveDF = spark.sql("select * from employee")

    hiveDF.printSchema()
    hiveDF.show()

    hiveDF.write
      .format("pulsar")
      .option("service.url", "pulsar://localhost:6650")
      .option("admin.url", "http://localhost:8088")
      .option("topic", "hive-employee")
      .save()
  }
}
