package org.eflerrr.sfp.sparkjobs.verifier;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.eflerrr.sfp.sparkjobs.verifier.service.ValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.reflect.ClassTag$;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

public class Verifier {

    private static final String SECRETS_MASTER_KEY = "SECRETS-MASTER-KEY";
    public static final Logger logger = LoggerFactory.getLogger(Verifier.class);

    public static void run() throws TimeoutException, StreamingQueryException {

        SparkSession spark = SparkSession
                .builder()
                .appName("Sensors Streaming Verifier")
                .config("spark.log.level", "INFO")
                .getOrCreate();

        var validatorService = new ValidatorService(
                spark.sparkContext().broadcast(
                        new HashMap<>(), ClassTag$.MODULE$.apply(Map.class))
        );

        // ------------ device secrets ------------ //

        var scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable refreshSecrets = () -> {
            Dataset<Row> deviceSecrets = spark.read()
                    .format("jdbc")
                    .option("url", "jdbc:postgresql://postgresql:5432/sensor_flow_platform")
                    .option("dbtable", "(SELECT device_id, pgp_sym_decrypt(secret, '%s') AS decrypt_secret FROM device) AS device_secrets"
                            .formatted(SECRETS_MASTER_KEY))
                    .option("user", "admin")
                    .option("password", "password")
                    .option("driver", "org.postgresql.Driver")
                    .load();

            Map<String, String> secretsMap = new HashMap<>();
            for (var row : deviceSecrets.collectAsList()) {
                String device = row.getAs("device_id");
                String secret = row.getAs("decrypt_secret");
                secretsMap.put(device, secret);
            }

            var bc = spark.sparkContext().broadcast(
                    secretsMap, ClassTag$.MODULE$.apply(Map.class));
            validatorService.updateBroadcast(bc);
            logger.info("Secrets are successfully updated!");
        };
        scheduler.scheduleAtFixedRate(refreshSecrets, 0, 10, TimeUnit.SECONDS);

        // ------------ verify ------------ //

        Dataset<Row> kafkaStreamRaw = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("kafka.security.protocol", "SASL_PLAINTEXT")
                .option("kafka.sasl.mechanism", "PLAIN")
                .option("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"admin\" password=\"admin-pass\";")
                .option("subscribe", "sensors-data-raw")
                .option("startingOffsets", "latest")
                .load();

        StructType schema = new StructType()
                .add("device_id", DataTypes.StringType)
                .add("metric_name", DataTypes.StringType)
                .add("metric_value", DataTypes.DoubleType)
                .add("src_timestamp", DataTypes.TimestampType)
                .add("signature", DataTypes.StringType);

        Dataset<Row> metricsDF = kafkaStreamRaw.selectExpr("CAST(value AS STRING)")
                .select(from_json(
                        col("value"), schema)
                        .alias("data"))
                .select("data.*");

        var udfName = "validateData";
        spark.udf().register(udfName, udf(
                (String deviceId, String metricName, Double metricValue, Timestamp srcTimestamp, String signature)
                        -> validatorService.validateData(deviceId, metricName, metricValue, srcTimestamp, signature),
                DataTypes.BooleanType));

        Dataset<Row> verifiedDF = metricsDF.filter(
                expr("%s(device_id, metric_name, metric_value, src_timestamp, signature) = true".formatted(udfName)));
        Dataset<Row> invalidDF = metricsDF.filter(
                expr("%s(device_id, metric_name, metric_value, src_timestamp, signature) = false".formatted(udfName)));

        Dataset<Row> verifiedData = verifiedDF.selectExpr(
                "CAST(null AS STRING) as key",
                "to_json(struct(*)) as value"
        );
        Dataset<Row> invalidData = invalidDF.selectExpr(
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
