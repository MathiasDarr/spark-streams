import findspark
findspark.init()
import pyspark as ps
import os
import boto3


def getSparkInstance():
    java8_location= '/usr/lib/jvm/java-8-openjdk-amd64' # Set your own
    os.environ['JAVA_HOME'] = java8_location
    spark = ps.sql.SparkSession.builder \
        .master("local[4]") \
        .appName("individual") \
        .getOrCreate()
    return spark


def download_parquet_files():
    s3 = boto3.resource('s3')
    tweets_bucket = s3.Bucket('dakobed-tweets')

    tweet_parquet_files = [file.key for file in list(tweets_bucket.objects.all()) if file.key.split('.')[-1]=='parquet']

    client = boto3.client('s3')
    for file in tweet_parquet_files:
        folder = 'tmp/' + file.split('/')[0]
        try:
            os.mkdir(folder)
        except Exception as e:
            pass
        hour = file.split('/')[1][:2]
        client.download_file('dakobed-tweets', file, '{}/{}.parquet'.format(folder, hour))


def create_tweets_dataframe():
    spark = getSparkInstance()

    parquet_files = []
    for folder in os.listdir('tmp'):
        for file in os.listdir('tmp/'+folder):
            parquet_files.append('tmp/{}/{}'.format(folder,file))

    dataframes = []
    for file in parquet_files:
        df = spark.read.parquet(file)
        dataframes.append(df)

    df = dataframes[0]
    for dataf in dataframes[1:]:
        df = df.union(dataf)
    return df

