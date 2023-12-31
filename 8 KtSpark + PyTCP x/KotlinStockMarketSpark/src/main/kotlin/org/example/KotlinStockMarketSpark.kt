package org.example

import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
import com.google.gson.JsonSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArraySerializer
import kotlinx.serialization.json.JsonElementSerializer
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.parse
import org.apache.spark.SparkConf
import org.apache.spark.streaming.Durations
import org.apache.spark.streaming.api.java.*
import scala.collection.parallel.ParIterableLike

@UnstableDefault
fun main() {
    // configurarea Spark
    val sparkConf = SparkConf().setMaster("local[2]").setAppName("Spark StockMarket")
    // initializarea contextului Spark
    val sparkContext = JavaStreamingContext(sparkConf, Durations.seconds(3))
    val items = sparkContext.socketTextStream("localhost", 9999)

    val processed = items
        .map{ obj -> {
            val objString = obj.toString()
            Json.parse(JsonObjectSerializer, objString)
        } }
        .filter {
            val objJson = it()
            //println(objJson["source"].toString())
            objJson["source"].toString() == "Yahoo"
        }
        .filter {
            val objJson = it()
            //println(objJson["summary"].toString())
            objJson["summary"].toString().length > 500
        }
        .flatMap {
            val objJson = it()

            val array: Array<String> = arrayOf("1","2","3")

            val url: String = objJson["url"].toString()
            val datetime: String = objJson["datetime"].toString()
            val headline: String = objJson["headline"].toString()

            array[0] = url + "\n"
            array[1] = datetime + "\n"
            array[2] = headline + "\n"

            array.iterator()
        }
//        .map{
//            val objJson = it()
//            "URL: " + objJson["url"].toString() + "\n" + "DATE: " + objJson["datetime"].toString() + "\n" + "TITLE: " + objJson["headline"].toString() + "\n"
//        }

    processed.print()

    sparkContext.start()
    sparkContext.awaitTermination()
}

