package org.mddarr.streaming;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LanguageIdentifier {
    HashMap<String, Set<String>> stop_word_map;
    public LanguageIdentifier(){
        SparkConf conf = new SparkConf().setMaster("local[*]").setAppName(KafkaSparkTweetsStream.class.getSimpleName());;
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<String> english = sc.textFile("file:///home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/english.txt",1);
        JavaRDD<String> spanish = sc.textFile("file:///home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/spanish.txt",1);
        JavaRDD<String> italian = sc.textFile("file:///home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/italian.txt",1);
        JavaRDD<String> french = sc.textFile("file:///home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/french.txt",1);
        Set<String> english_stop_words = getListFromIterator(english.collect().iterator());
        Set<String> french_stop_words = getListFromIterator(french.collect().iterator());
        Set<String> spanish_stop_words = getListFromIterator(spanish.collect().iterator());
        Set<String> italian_stop_words = getListFromIterator(italian.collect().iterator());

        stop_word_map = new HashMap<>();
        stop_word_map.put("english", english_stop_words);
        stop_word_map.put("spanish", spanish_stop_words);
        stop_word_map.put("french", french_stop_words);
        stop_word_map.put("italian", italian_stop_words);
        sc.stop();
        sc.close();
    }

    public HashMap<String, Set<String>> getStop_word_map() {
        return stop_word_map;
    }

    public Set<String> getListFromIterator(Iterator<String> iterator)
    {
        Iterable<String> iterable = () -> iterator;
        Set<String> set = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toSet());
        return set;
    }

}
