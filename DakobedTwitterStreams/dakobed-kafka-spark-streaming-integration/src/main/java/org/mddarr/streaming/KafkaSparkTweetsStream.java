package org.mddarr.streaming;

import com.google.common.collect.Sets;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;

import org.apache.spark.SparkConf;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;

import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.mddarr.avro.tweets.Tweet;
import scala.Tuple2;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import scala.collection.JavaConverters;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class KafkaSparkTweetsStream {
    private static Object BOOTSTRAP_SERVER;


    private static Consumer<Long, Tweet> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
        // Create the consumer using props.
        final Consumer<Long, Tweet> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("kafka-tweets"));
        return consumer;
    }

    private static  Map<String, Object> getKafkaParams(){
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        kafkaParams.put("key.deserializer", LongDeserializer.class);
        kafkaParams.put("value.deserializer", io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        kafkaParams.put("group.id", "use_a_separate_group_id_for_each_stream");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);
        kafkaParams.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        kafkaParams.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
        return kafkaParams;
    }

    private static void consumeTweets(){
        Consumer<Long, Tweet> consumer = createConsumer();

        final int giveUp = 100;
        int noRecordsCount = 0;
        while (true) {
            final ConsumerRecords<Long, Tweet> consumerRecords = consumer.poll(Duration.ofMillis(100));
            if (consumerRecords.count() == 0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp) break;
                else continue;
            }
            consumerRecords.forEach(record -> {
                System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                        record.key(), record.value(),
                        record.partition(), record.offset());
            });
            consumer.commitAsync();
        }
        consumer.close();
        System.out.println("DONE");
    }

    public static String identifyLanguage(String content, HashMap<String, Set<String>> stop_words_map){

        String[] words = content.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");

        Set<String> wordsSet = new HashSet<>(Arrays.asList(words));
        HashMap<String, Integer> ratios = new HashMap<>();
        Set<String> intersecion;

        for (Map.Entry<String, Set<String>> entry : stop_words_map.entrySet()) {
            intersecion = Sets.intersection(wordsSet, entry.getValue());
            ratios.put(entry.getKey(), intersecion.size());
        }

        String current_maximum_language = "-1";
        Integer current_maximum = -1;
        for (Map.Entry<String, Integer> entry : ratios.entrySet()) {
            if(entry.getValue() > current_maximum){
                current_maximum = entry.getValue();
                current_maximum_language = entry.getKey();
            }
        }
        return current_maximum_language;
    }


    private static void streamTweetsMain(HashMap<String, Set<String>> stop_words_map) throws InterruptedException {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
        Collection<String> topics = Collections.singletonList("kafka-tweets");

        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster("local[*]");
        sparkConf.setAppName("TweetsApplication");

//        SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();

        JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(1));

        Map<String, Object> kafkaParams = getKafkaParams();

        JavaInputDStream<ConsumerRecord<Long, Tweet>> tweets = KafkaUtils.createDirectStream(
                streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.<Long, Tweet> Subscribe(topics, kafkaParams));

        JavaDStream<Tweet> lines = tweets.map(ConsumerRecord::value);
        JavaDStream<String> tweet_content = lines.map((Function<Tweet, String>) Tweet::getTweetContent);

//        JavaDStream<String> spanish_tweets = tweet_content.filter((Function<String, Boolean>) x-> identifyLanguage(x, stop_words_map).equals("spanish"));
//        JavaDStream<String> english_tweets = tweet_content.filter((Function<String, Boolean>) x-> identifyLanguage(x, stop_words_map).equals("english"));
//        JavaDStream<String> french_tweets = tweet_content.filter((Function<String, Boolean>) x-> identifyLanguage(x, stop_words_map).equals("french"));
//        JavaDStream<String> italian_tweets = tweet_content.filter((Function<String, Boolean>) x-> identifyLanguage(x, stop_words_map).equals("italian"));
//
//        spanish_tweets.print();

        JavaDStream<Tweet> spanish_raw_tweets = lines.filter((Function<Tweet, Boolean>)  tweet-> identifyLanguage(tweet.getTweetContent(), stop_words_map).equals("spanish"));
        JavaDStream<Tweet> english_tweets = lines.filter((Function<Tweet, Boolean>)  tweet-> identifyLanguage(tweet.getTweetContent(), stop_words_map).equals("english"));

        JavaDStream<String> english_words = english_tweets.flatMap((FlatMapFunction<Tweet, String>) x -> Arrays.asList(x.getTweetContent().split(" ")).iterator());
        english_words.print();
        english_words.foreachRDD((rdd, time) -> {
            SparkSession spark = JavaSparkSessionSingleton.getInstance(rdd.context().getConf());

            // Convert JavaRDD[String] to JavaRDD[bean class] to DataFrame
            JavaRDD<JavaRow> rowRDD = rdd.map(word -> {
                JavaRow record = new JavaRow();
                System.out.println("word "+  word);
                record.setWord(word);
                return record;
            });
            Dataset<Row> wordsDataFrame = spark.createDataFrame(rowRDD, JavaRow.class);

            // Creates a temporary view using the DataFrame
            wordsDataFrame.createOrReplaceTempView("words");

            // Do word count on table using SQL and print it
            Dataset<Row> wordCountsDataFrame = spark.sql("select word, count(*) as total from words group by word");
            logger.info("========= " + time + "=========");
            wordCountsDataFrame.show();
        });
//        tweet_content.foreachRDD((rdd, time) -> {
//            // Get the singleton instance of SparkSession
//            SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();
//
//            // Convert RDD[String] to RDD[case class] to DataFrame
//            JavaRDD<JavaRow> rowRDD = rdd.map(word -> {
//                JavaRow record = new JavaRow();
//                record.setWord(word);
//                return record;
//            });
//            Dataset<Row> wordsDataFrame = spark.createDataFrame(rowRDD, JavaRow.class);
//
//            // Creates a temporary view using the DataFrame
//            wordsDataFrame.createOrReplaceTempView("words");
//
//            // Do word count on table using SQL and print it
//            Dataset<Row> wordCountsDataFrame = spark.sql("select word, count(*) as total from words group by word");
//            wordCountsDataFrame.show();
//        });

//        Dataset<Row> peopleDataFrame = spark.createDataFrame(tweet_content);
//        identified_language_stream.print();
        streamingContext.start();
        streamingContext.awaitTermination();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
//        Parser parser = new Parser();

        Logger.getLogger("org")
                .setLevel(Level.OFF);
        Logger.getLogger("akka")
                .setLevel(Level.OFF);
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();

        LanguageIdentifier languageIdentifier = new LanguageIdentifier();
        HashMap<String, Set<String>> stop_words_map = languageIdentifier.getStop_word_map();

        streamTweetsMain(stop_words_map);



    }
}

/** Lazily instantiated singleton instance of SparkSession */
class JavaSparkSessionSingleton {
    private static transient SparkSession instance = null;
    public static SparkSession getInstance(SparkConf sparkConf) {
        if (instance == null) {
            instance = SparkSession
                    .builder()
                    .config(sparkConf)
                    .getOrCreate();
        }
        return instance;
    }
}


//    Set<String> firstset = stop_words_map.get("english");
//        Iterator<String> itr = firstset.iterator();

//        while(itr.hasNext()){
//                System.out.println(itr.next());
//                }

//
//        String first_text = "Why hello there! my Friends you# are. the dumbest";
//        String[] words = first_text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//
//        String language = identifyLanguage(words, stop_words_map);


//        SparkSession spark = SparkSession
//                .builder()
//                .appName("Java Spark SQL basic example")
//                .config("spark.some.config.option", "some-value")
//                .getOrCreate();

//        SparkContext sc = spark.sparkContext();
//        List<Integer> seqNumList = IntStream.rangeClosed(10, 20).boxed().collect(Collectors.toList());
//        RDD<Integer> numRDD = sc.parallelize(JavaConverters.asScalaIteratorConverter(seqNumList.iterator()).asScala()
//                        .toSeq(), 2, scala.reflect.ClassTag$.MODULE$.apply(Integer.class));
//        logger.info("First Log " + numRDD.count());

//        JavaDStream<String> words = tweets.flatMap(
//                (FlatMapFunction<Tweet, String>) x -> Arrays.asList(x.getTweetContent().split(" ")).iterator()
//        );

//        JavaDStream<String> tweetStream = tweets.flatMap((FlatMapFunction<Tweet, String>) tweet-> tweet.getTweetContent()).iteratror();
//        JavaDStream<String> words = tweets.flatMap(
//                (FlatMapFunction<Tweet, String>) x -> Arrays.asList(x.getTweetContent().split(" ")).iterator()
//        );


//        JavaPairDStream<Long, Tweet> tweets = messages.mapToPair(record -> new Tuple2<>(record.key(), record.value()));
//        tweets.print();



//        Tweet tweet = new Tweet();
//        tweet.getTweetContent()
//      Split each line into words
//        JavaDStream<String> words = messages.flatMap((FlatMapFunction<Tweet, String>) tweet -> Arrays.asList(tweet.getTweetContent().split(" ")).iterator());
//
//        JavaPairDStream<String, Integer> pairs = words.mapToPair(
//                (PairFunction<String, String, Integer>) s -> new Tuple2<>(s, 1));

//        JavaPairInputDStream<String, String> directKafkaStream = KafkaUtils.createDirectStream(streamingContext,
//                String.class, String.class, LongDeserializer.class, KafkaAvroDeserializer.class, kafkaParams, topics);

//
//        JavaInputDStream<ConsumerRecord<Long, Tweet>> directKafkaStream = KafkaUtils.createDirectStream(
//                streamingContext,
//                LocationStrategies.PreferConsistent(),
//                ConsumerStrategies.<Long, Tweet>Assign(fromOffsets.keySet(), kafkaParams, fromOffsets)
//        );





//        JavaDStream<String> lines = results.map(tuple2 -> tuple2._2());
//
//        JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(x.split("\\s+"))
//                .iterator());
//
//        JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s, 1))
//                .reduceByKey((i1, i2) -> i1 + i2);
//        wordCounts.print();
//        wordCounts.foreachRDD(javaRdd -> {
//            Map<String, Integer> wordCountMap = javaRdd.collectAsMap();
//            for (String key : wordCountMap.keySet()) {
//                List<Word> wordList = Arrays.asList(new Word(key, wordCountMap.get(key)));
//                JavaRDD<Word> rdd = streamingContext.sparkContext()
//                        .parallelize(wordList);
//                System.out.println("The word is " + key);
//            }
//        });
