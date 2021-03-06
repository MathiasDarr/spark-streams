import findspark
findspark.init()
import pyspark as ps
from pyspark.sql.types import LongType, IntegerType, ArrayType, StringType, BooleanType
from pyspark.ml.clustering import KMeans
import os
import numpy as np
import string
import re
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
from nltk.stem.porter import PorterStemmer
from pyspark.ml.clustering import KMeans
from pyspark.ml.feature import CountVectorizer, IDF
from pyspark.sql.functions import udf
import unicodedata
import sys
from pyspark.sql import Row
from nltk import wordpunct_tokenize


PUNCTUATION = set(string.punctuation)
STOPWORDS = set(stopwords.words('english'))


def getSparkInstance():
    """
    @return: Return Spark session
    """
    java8_location= '/usr/lib/jvm/java-8-openjdk-amd64' # Set your own
    os.environ['JAVA_HOME'] = java8_location

    spark = ps.sql.SparkSession.builder \
        .master("local[4]") \
        .appName("individual") \
        .getOrCreate()
    return spark


def create_tweets_dataframe():
    """
    @return: Spark Dataframes with all tweets loaded from Parquet
    """
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


def tokenize(text):
    """
    :param text: String
    :return: a list of tokenized words stripped of punctuation
    """
    try:
        regex = re.compile('<.+?>|[^a-zA-Z]')
        clean_txt = regex.sub(' ', text)
        tokens = clean_txt.split()
        lowercased = [t.lower() for t in tokens]
        no_punctuation = []
        for word in lowercased:
            punct_removed = ''.join([letter for letter in word if not letter in PUNCTUATION])
            no_punctuation.append(punct_removed)
        no_stopwords = [w for w in no_punctuation if not w in STOPWORDS]

        STEMMER = PorterStemmer()
        stemmed = [STEMMER.stem(w) for w in no_stopwords]
        return [w for w in stemmed if w]
    except Exception as e:
        return ["failure"]


def _calculate_languages_ratios(text):
    """
    Calculate probability of given text to be written in several languages and
    return a dictionary that looks like {'french': 2, 'spanish': 4, 'english': 0}

    @param text: Text whose language want to be detected
    @type text: str

    @return: Dictionary with languages and unique stopwords seen in analyzed text
    @rtype: dict
    """
    languages_ratios = {}
    '''
    nltk.wordpunct_tokenize() splits all punctuations into separate tokens

    >>> wordpunct_tokenize("That's thirty minutes away. I'll be there in ten.")
    ['That', "'", 's', 'thirty', 'minutes', 'away', '.', 'I', "'", 'll', 'be', 'there', 'in', 'ten', '.']
    '''

    tokens = wordpunct_tokenize(text)
    words = [word.lower() for word in tokens]

    # Compute per language included in nltk number of unique stopwords appearing in analyzed text
    for language in ['english','spanish', 'italian', 'french']:
        stopwords_set = set(stopwords.words(language))
        words_set = set(words)
        common_elements = words_set.intersection(stopwords_set)
        languages_ratios[language] = len(common_elements)  # language "score"
    return languages_ratios


def detect_language(text):
    """
    Calculate probability of given text to be written in several languages and
    return the highest scored.
    It uses a stopwords based approach, counting how many unique stopwords
    are seen in analyzed text.
    @param text: Text whose language want to be detected
    @type text: str
    @return: Most scored language guessed
    @rtype: str
    """

    ratios = _calculate_languages_ratios(text)
    most_rated_language = max(ratios, key=ratios.get)
    return most_rated_language


@udf(returnType=ArrayType(StringType()))
def tokenize_udf(x):
    return tokenize(x)


detect_language_udf = udf(lambda content: detect_language(content), StringType())


df = create_tweets_dataframe()
filtered_dataframe = df.filter(df['content'] != 'null')
tweets_dataframe = filtered_dataframe.select('content', 'date', 'location', 'username')
tokenized_tweets_df = tweets_dataframe.select('content', 'date', 'location', 'username', tokenize_udf('content').alias('tokenized_content'))
identify_language_df = tokenized_tweets_df.select('content', 'date', 'location', 'username', 'tokenized_content', detect_language_udf('content').alias('language'))

english_virus_tweets = identify_language_df.filter(identify_language_df['language'] =='english')
english_virus_tweets = english_virus_tweets.select('content', 'date', 'location', 'username', 'tokenized_content')

italian_virus_tweets = identify_language_df.filter(identify_language_df['language'] =='italian')
italian_virus_tweets = italian_virus_tweets.select('content', 'date', 'location', 'username', 'tokenized_content')


cv = CountVectorizer(inputCol="tokenized_content", outputCol="features")
model = cv.fit(tokenized_tweets_df)

cv = CountVectorizer(inputCol="bow", outputCol="vector_tf", vocabSize=vocabSize_, minDF=minDF_)
cv_model = cv.fit(df_tokens)
df_features_tf = cv_model.transform(df_tokens)

idf = IDF(inputCol="vector_tf", outputCol="features")
idfModel = idf.fit(df_features_tf)
df_features = idfModel.transform(df_features_tf)


idf = IDF(inputCol="vector_tf", outputCol="features")
idfModel = idf.fit(df_features_tf)
df_features = idfModel.transform(df_features_tf)



result = model.transform(tokenized_tweets_df)
result.show(truncate=False)
