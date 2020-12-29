#!/bin/bash
$SPARK_HOME/bin/spark-submit --class org.mddarr.streaming.KafkaSparkTweetsStream dakobed-kafka-spark-streaming-integration/target/dakobed-kafka-spark-streaming-integration-1.0-SNAPSHOT-jar-with-dependencies.jar \
-- files
