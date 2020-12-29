### Dakobed Twitter Analysis Pipeline

### Dakobed Twitter Pipeline

This directory contains a data pipeline project in which I stream & process tweets.  


### Dakobed-twitter-producer

Multithreaded Java application which ingests tweets using the twitter4j library and adds them to an array blocking queue
in on thread which are produced to an ElasticSearch index in the other thread.  

java -jar target/dakobed-twitter-producer-1.0-SNAPSHOT-jar-with-dependencies.jar virus. localhost 29200

### Dakobed Twitter Analysis Pipeline

In this directory I define an Airflow DAG which makes use of several Python Operators whose tasks run in sequence to 
scan the ElasticSearch tweets index (populated by the Java program in dakobed-twitter-producer), and then to delete the
the tweets, freeing memory.  The tweets are loaded into a Spark dataframe, saved to parquet, and then uploaded to S3.   


### dakobed-twitter-service
AWS ElasticSearch Service

Verify that the ES cluster is healthy -> replacing the ES DNS  

curl -u 'master-user:1!Master-user-password'  'https://search-dakobedes-o5fqopyonjvcuzkvpeyoezfgey.us-west-2.es.amazonaws.com/_cat/health?v'
curl -u 'master-user:1!Master-user-password'  'https://search-dakobedes-o5fqopyonjvcuzkvpeyoezfgey.us-west-2.es.amazonaws.com/_aliases'

