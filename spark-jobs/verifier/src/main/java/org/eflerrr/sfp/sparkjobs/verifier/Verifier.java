package org.eflerrr.sfp.sparkjobs.verifier;

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

public class Verifier {
    public static void run() throws TimeoutException, StreamingQueryException {

        SparkSession spark = SparkSession
                .builder()
                .appName("Sensors Streaming Verifier")
                .config("spark.log.level", "INFO")
                .getOrCreate();

        Dataset<Row> kafkaStreamRaw = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("kafka.security.protocol", "SASL_PLAINTEXT")
                .option("kafka.sasl.mechanism", "PLAIN")
                .option("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"producer\" password=\"producer-pass\";")
                .option("subscribe", "sensors-data-raw")
                .option("startingOffsets", "earliest")
                .load();

        StructType sensorsDataSchema = new StructType()
                .add("device_id", DataTypes.StringType)
                .add("metric_name", DataTypes.StringType)
                .add("metric_value", DataTypes.DoubleType)
                .add("src_timestamp", DataTypes.TimestampType)
                .add("signature", DataTypes.StringType);

        Dataset<Row> metricsDF = kafkaStreamRaw.selectExpr("CAST(value AS STRING)")
                .select(from_json(
                        col("value"), sensorsDataSchema)
                        .alias("data"))
                .select("data.*");

        StreamingQuery query = metricsDF.writeStream()
                .outputMode("append")
                .trigger(Trigger.ProcessingTime("3 seconds"))
                .foreachBatch((batchDF, batchId) -> {
                    batchDF.write()                     // todo! configs
                            .format("jdbc")
                            .option("url", "jdbc:postgresql://timescaledb:5432/sensor_flow_platform")
                            .option("dbtable", "sensors_metrics")
                            .option("user", "admin")
                            .option("password", "password")
                            .option("driver", "org.postgresql.Driver")
                            .mode("append")
                            .save();
                })
                .option("checkpointLocation", "s3a://spark-bucket/checkpoints/visualizer")
                .start();

        query.awaitTermination();

    }
}
