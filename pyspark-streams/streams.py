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

inputDF.show()