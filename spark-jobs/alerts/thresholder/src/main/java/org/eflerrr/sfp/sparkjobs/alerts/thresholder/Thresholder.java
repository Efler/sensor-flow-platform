package org.eflerrr.sfp.sparkjobs.alerts.thresholder;

import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.Rule;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.ThresholdType;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.service.AlertService;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.utils.RuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.reflect.ClassTag$;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;
import static org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.AlertType.BROKEN_LOWER_THRESHOLD;
import static org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.AlertType.BROKEN_UPPER_THRESHOLD;

public class Thresholder {
    public static final Logger logger = LoggerFactory.getLogger(Thresholder.class);

    public static void run() throws TimeoutException, StreamingQueryException {
        SparkSession spark = SparkSession
                .builder()
                .appName("Sensors Streaming Thresholder")
                .config("spark.log.level", "INFO")
                .getOrCreate();

        StructType schema = new StructType()
                .add("device_id", DataTypes.StringType)
                .add("metric_name", DataTypes.StringType)
                .add("metric_value", DataTypes.DoubleType)
                .add("src_timestamp", DataTypes.TimestampType);
        StructType thresholds = new StructType()
                .add("lower", DataTypes.DoubleType, true)
                .add("upper", DataTypes.DoubleType, true);

        Dataset<Row> stream = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("kafka.security.protocol", "SASL_PLAINTEXT")
                .option("kafka.sasl.mechanism", "PLAIN")
                .option("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"admin\" password=\"admin-pass\";")
                .option("subscribe", "sensors-data-verified")
                .option("startingOffsets", "latest")
                .load();
        Dataset<Row> data = stream.selectExpr("CAST(value AS STRING)")
                .select(from_json(col("value"), schema).alias("data"))
                .select("data.*");

        AlertService alertService = new AlertService();
        spark.udf().register("computeThresholds", udf(
                (String deviceId, String metricName, Timestamp ts) ->
                        alertService.computeThresholds(deviceId, metricName, ts),
                thresholds
        ));
        spark.udf().register("isAlert", udf(
                (String deviceId, String metricName, Double metricValue, Timestamp ts,
                 Double lowerThreshold, Double upperThreshold) ->
                        alertService.isAlert(deviceId, metricName, metricValue, ts, lowerThreshold, upperThreshold),
                DataTypes.BooleanType));

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable refreshRules = () -> {
            Dataset<Row> devices = spark.read()
                    .format("jdbc")
                    .option("url", "jdbc:postgresql://postgresql:5432/sensor_flow_platform")
                    .option("dbtable", "device")
                    .option("user", "admin")
                    .option("password", "password")
                    .option("driver", "org.postgresql.Driver")
                    .load()
                    .select("device_id", "model_id", "environment_temp", "usage_hours", "install_date");
            Dataset<Row> modelParams = spark.read()
                    .format("jdbc")
                    .option("url", "jdbc:postgresql://postgresql:5432/sensor_flow_platform")
                    .option("dbtable", "model_params")
                    .option("user", "admin")
                    .option("password", "password")
                    .option("driver", "org.postgresql.Driver")
                    .load()
                    .select("model_id", "metric_name", "threshold_type", "a0", "a1", "a2", "a3");

            List<Row> deviceRows = devices.collectAsList();
            List<Row> paramRows = modelParams.collectAsList();
            Map<String, Rule> map = new HashMap<>();
            RuleBuilder ruleBuilder = RuleBuilder.getInstance();
            for (Row m : deviceRows) {
                String deviceId = m.getAs("device_id");
                String modelId = m.getAs("model_id");
                LocalDate installDate = m.<Timestamp>getAs("install_date")
                        .toLocalDateTime().toLocalDate();
                double envTemp = m.getAs("environment_temp");
                double usageHrs = m.getAs("usage_hours");
                for (Row p : paramRows) {
                    if (!modelId.equals(p.getAs("model_id"))) {
                        continue;
                    }
                    String metricName = p.getAs("metric_name");
                    var thresholdType = ThresholdType.fromString(p.getAs("threshold_type"));
                    double a0 = p.getAs("a0");
                    double a1 = p.getAs("a1");
                    double a2 = p.getAs("a2");
                    double a3 = p.getAs("a3");
                    var key = deviceId + "#" + metricName + "#" + thresholdType;
                    var value = ruleBuilder.withDeviceId(deviceId)
                            .withModelId(modelId)
                            .withInstallDate(installDate)
                            .withEnvTemp(envTemp)
                            .withUsageHours(usageHrs)
                            .withA0(a0)
                            .withA1(a1)
                            .withA2(a2)
                            .withA3(a3)
                            .withType(thresholdType)
                            .build();
                    map.put(key, value);
                    logger.info("Rule updated:  {} | {}", key, value);
                }
            }

            Broadcast<Map<String, Rule>> bc = spark.sparkContext().broadcast(
                    map, ClassTag$.MODULE$.apply(Map.class));
            alertService.updateBroadcast(bc);
        };
        scheduler.scheduleAtFixedRate(refreshRules, 0, 10, TimeUnit.SECONDS);

        Dataset<Row> withThresholds = data.withColumn(
                "thresholds",
                callUDF("computeThresholds",
                        col("device_id"),
                        col("metric_name"),
                        col("src_timestamp")));
        Dataset<Row> alerts = withThresholds.filter(
                expr("isAlert(device_id, metric_name, metric_value, src_timestamp, thresholds.lower, thresholds.upper)"));
        Dataset<Row> alertsEnriched = alerts.withColumn(
                "alert_type",
                when(col("metric_value").gt(col("thresholds.upper")),
                        lit(BROKEN_UPPER_THRESHOLD))
                        .otherwise(lit(BROKEN_LOWER_THRESHOLD)));

        StreamingQuery query = alertsEnriched.selectExpr(
                        "CAST(null AS STRING) AS key",
                        "to_json(struct(*)) AS value")
                .writeStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-broker-1:9192,kafka-broker-2:9292,kafka-broker-3:9392")
                .option("kafka.security.protocol", "SASL_PLAINTEXT")
                .option("kafka.sasl.mechanism", "PLAIN")
                .option("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"admin\" password=\"admin-pass\";")
                .option("topic", "sensors-data-alerts")
                .option("checkpointLocation", "s3a://spark-bucket/checkpoints/thresholder")
                .start();

        query.awaitTermination();
    }
}
