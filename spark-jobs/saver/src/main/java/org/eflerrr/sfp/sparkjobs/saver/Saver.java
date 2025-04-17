package org.eflerrr.sfp.sparkjobs.saver;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.from_json;

public class Saver {
    public static void run() throws TimeoutException, StreamingQueryException {

        SparkSession spark = SparkSession
                .builder()
                .appName("Sensors Streaming Saver")
                .config("spark.log.level", "INFO")
                .getOrCreate();

        Dataset<Row> kafkaStream = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("subscribe", "sensors-data")
                .option("startingOffsets", "latest")
                .load();

        StructType sensorsDataSchema = new StructType()
                .add("device_id", DataTypes.StringType)
                .add("metric_name", DataTypes.StringType)
                .add("metric_value", DataTypes.DoubleType)
                .add("src_timestamp", DataTypes.TimestampType);

        Dataset<Row> metricsDF = kafkaStream.selectExpr("CAST(value AS STRING)")
                .select(from_json(
                        col("value"), sensorsDataSchema)
                        .alias("data"))
                .select("data.*");

        StreamingQuery query = metricsDF.writeStream()
                .trigger(Trigger.ProcessingTime("10 seconds"))
                .foreachBatch((batchDF, batchId) -> {
                    batchDF.write()                     // todo! configs
                            .format("parquet")
                            .option("path", "s3a://spark-bucket/sensors-data/")
                            .mode("append")
                            .save();
                    System.out.println("Writing batch " + batchId + " to S3");
                })
                .option("checkpointLocation", "s3a://spark-bucket/checkpoints/saver")
                .start();

        query.awaitTermination();

    }
}
