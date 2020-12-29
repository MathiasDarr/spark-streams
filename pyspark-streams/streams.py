import findspark
findspark.init()
import pyspark as ps
from pyspark import SparkContext, SparkConf
from pyspark.sql.types import StructField, StructType
from pyspark.sql.types import TimestampType, StringType

# Path to our 20 JSON files
inputPath = "data/"


def getSparkInstance():
    """
    @return: Return Spark session
    """
    # java8_location = '/usr/lib/jvm/java-8-openjdk-amd64' # Set your own
    # os.environ['JAVA_HOME'] = java8_location

    spark = ps.sql.SparkSession.builder \
        .master("local[4]") \
        .appName("individual") \
        .getOrCreate()
    return spark

spark = getSparkInstance()

schema = StructType([ StructField("time", TimestampType(), True),
                      StructField("customer", StringType(), True),
                      StructField("action", StringType(), True),
                      StructField("device", StringType(), True)])

inputDF = (
  spark
    .read
    .schema(schema)
    .json(inputPath)
)
inputDF = inputDF.dropna()
# inputDF.show()

actionsDF = (inputDF.groupBy(inputDF.action).count())
actionsDF.cache()

# Create temp table named 'iot_action_counts'
actionsDF.createOrReplaceTempView("iot_action_counts")

select_results = spark.sql("SELECT action, SUM(count) AS total_count FROM iot_action_counts group by action")
# select_results.show()


streamingDF = (
  spark
    .readStream
    .schema(schema)
    .option("maxFilesPerTrigger", 1)
    .json(inputPath)
)

print("THE STREAMINGDF IS STREAMING {}".format(streamingDF.isStreaming))

streamingDF.writeStream \
    .format("console") \
    .start()


spark.streams.awaitAnyTermination()

# streamingActionCountsDF = (
#   streamingDF
#     .groupBy(
#       streamingDF.action
#     )
#     .count()
# )
#
#
# spark.conf.set("spark.sql.shuffle.partitions", "2")
#
# # View stream in real-time
# query = (
#   streamingActionCountsDF
#     .writeStream
#     .format("memory")
#     .queryName("counts")
#     .outputMode("complete")
#     .start()
# )


