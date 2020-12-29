$SPARK_HOME/bin/spark-submit --class org.mddarr.streaming.WordCount  dakobed-kafka-spark-streaming-integration/target/dakobed-kafka-spark-streaming-integration-1.0-SNAPSHOT-jar-with-dependencies.jar localhost 9999
$SPARK_HOME/bin/spark-submit --class org.mddarr.streaming.KafkaSparkTweetsStream dakobed-kafka-spark-streaming-integration/target/dakobed-kafka-spark-streaming-integration-1.0-SNAPSHOT-jar-with-dependencies.jar

docker exec kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic tweets
docker exec -it kafka kafka-console-producer --topic=tweets --bootstrap-server localhost:9092
docker exec -it kafka kafka-console-consumer --topic=tweets --bootstrap-server localhost:9092


Word Count Example
open 
nc -lk 9999
