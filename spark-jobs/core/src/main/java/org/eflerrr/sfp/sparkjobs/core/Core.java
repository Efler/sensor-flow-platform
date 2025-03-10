package org.eflerrr.sfp.sparkjobs.core;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.from_json;

public class Core {
    public static void run() throws TimeoutException, StreamingQueryException {

        SparkSession spark = SparkSession
                .builder()
                .appName("Sensors Streaming")
                .config("spark.log.level", "WARN")
                .getOrCreate();

        Dataset<Row> kafkaStream = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("subscribe", "sensors-data")
                .option("startingOffsets", "earliest")
                .load();

        Dataset<Row> parsedData = kafkaStream
                .selectExpr("CAST(value AS STRING) as json")
                .select(from_json(
                        col("json"), new StructType().add("device_id", "string"))
                        .alias("data"))
                .select("data.device_id");

//        Dataset<Row> filteredData = parsedData
//                .filter(col("metric_value").gt(100.0));

        StreamingQuery query = parsedData.writeStream()
                .outputMode("append")
                .format("console")
                .start();

        query.awaitTermination();

    }
}
