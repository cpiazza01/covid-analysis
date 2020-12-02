package blue

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{explode, when}

object DataFrameManipulator {
  def caseJoin(spark: SparkSession, regionDF: DataFrame, caseDF: DataFrame): DataFrame ={
    import spark.implicits._

    val regionDict = regionDF
      .select($"name", explode($"countries") as "country2")

    caseDF
      .select( $"date", $"country", $"total_cases", $"new_cases")
      .join(regionDict, $"country" === $"country2")
      .drop($"country2")
      .withColumn("new_cases", when($"new_cases"==="NULL", 0).otherwise($"new_cases"))
      .withColumn("total_cases", when($"total_cases"==="NULL", 0).otherwise($"total_cases"))
  }

   def econJoin(spark: SparkSession, regionDF: DataFrame, econDF: DataFrame): DataFrame ={
    import spark.implicits._

    val regionDict = regionDF
      .select($"name", explode($"countries") as "country")
      .select($"name" as "region", $"country" as "country2")

    econDF
      .join(regionDict, $"country" === $"country2")
      .select($"2020" as "2020_GDP", $"2019" as "2019_GDP", $"region", $"country")
      .where($"WEO Subject Code" === "PPPGDP")
      .drop($"country2")
  }
  

  def joinCaseEcon(spark: SparkSession, caseDF: DataFrame, econDF: DataFrame): DataFrame = {
    import spark.implicits._
    econDF.createOrReplaceTempView("econDFTemp")
    caseDF.createOrReplaceTempView("caseDFTemp")
    val caseEconDF = spark.sql(
      "SELECT e.region, c.country, e.2020_GDP, e.2019_GDP, c.total_cases, c.new_cases, c.date " +
        " FROM econDFTemp e JOIN caseDFTemp c " +
        "ON e.country == c.country " +
        "ORDER BY region, 2020_GDP")

    caseEconDF
  }
}