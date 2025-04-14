package org.eflerrr.sfp.sparkjobs.verifier;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.eflerrr.sfp.sparkjobs.verifier.service.ValidatorService;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

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
                        "username=\"admin\" password=\"admin-pass\";")
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

        List<Row> secretRows = List.of(
                RowFactory.create("device_228", "secret_228"));      // todo! device secrets!
        StructType secretSchema = new StructType()
                .add("device_id", DataTypes.StringType)
                .add("secret", DataTypes.StringType);
        Dataset<Row> secretsDF = spark.createDataFrame(secretRows, secretSchema);

        Dataset<Row> resultDF = metricsDF.join(secretsDF, "device_id", "left");

        var udfName = "validateData";
        spark.udf().register(udfName, ValidatorService.getUDF(), DataTypes.BooleanType);

        Dataset<Row> verifiedDF = resultDF.filter(
                expr("%s(device_id, metric_name, metric_value, src_timestamp, signature, secret) = true".formatted(udfName)));
        Dataset<Row> invalidDF = resultDF.filter(
                expr("%s(device_id, metric_name, metric_value, src_timestamp, signature, secret) = false".formatted(udfName)));

        Dataset<Row> outputVerifiedDF = verifiedDF.drop("secret");
        Dataset<Row> outputInvalidDF = invalidDF.drop("secret");

        Dataset<Row> verifiedData = outputVerifiedDF.selectExpr(
                "CAST(null AS STRING) as key",
                "to_json(struct(*)) as value"
        );
        Dataset<Row> invalidData = outputInvalidDF.selectExpr(
                "CAST(null AS STRING) as key",
                "to_json(struct(*)) as value"
        );

        StreamingQuery verifiedQuery = verifiedData.writeStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("kafka.security.protocol", "SASL_PLAINTEXT")
                .option("kafka.sasl.mechanism", "PLAIN")
                .option("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"admin\" password=\"admin-pass\";")
                .option("topic", "sensors-data-verified")
                .option("checkpointLocation", "s3a://spark-bucket/checkpoints/verifier/verified")
                .start();

        StreamingQuery invalidQuery = invalidData.writeStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("kafka.security.protocol", "SASL_PLAINTEXT")
                .option("kafka.sasl.mechanism", "PLAIN")
                .option("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"admin\" password=\"admin-pass\";")
                .option("topic", "sensors-data-dlq")
                .option("checkpointLocation", "s3a://spark-bucket/checkpoints/verifier/dlq")
                .start();

        verifiedQuery.awaitTermination();
        invalidQuery.awaitTermination();
    }
}
