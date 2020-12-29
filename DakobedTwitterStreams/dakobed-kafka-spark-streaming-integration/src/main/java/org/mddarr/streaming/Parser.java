package org.mddarr.streaming;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.*;

public class Parser {

    HashMap<String, Set<String>> stopwords;

    public Parser(String filepath) throws IOException {
//        String english_stop_words = "/home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/english.txt";
//        String spanish_stop_words = "/home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/spanish.txt";
//        String italian_stop_words = "/home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/italian.txt";
//        String french_stop_words = "/home/mddarr/data/DakobedBard/dakobed-twitter/DakobedTwitterStreams/dakobed-kafka-spark-streaming-integration/src/main/resources/french.txt";
//        String[] languages = {english_stop_words, spanish_stop_words, italian_stop_words, french_stop_words};
        String [] languages = {filepath};
        stopwords = generateStopWordsMap(languages);
    }

    private  String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br  = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public HashMap<String, Set<String>> generateStopWordsMap(String[] languages) throws IOException {
        Class clazz = Parser.class;
        HashMap<String, Set<String>> stop_words_map= new HashMap<>();
        for(String language:languages){
            InputStream inputStream = clazz.getResourceAsStream(language);
            String data = readFromInputStream(inputStream);
            String[] words = data.split("\n");
            stop_words_map.put(language, new HashSet<>(Arrays.asList(words)));
        }
        return stop_words_map;
    }
}
