package com.atguigu.dataloader

import java.net.InetAddress

import com.atguigu.scala.model.{ESConfig, MongoConfig, Movie, MovieRating, Tag}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import com.atguigu.java.model.Constant._




object DataLoader {

  val MOVIE_DATA_PATH = "C:\\IDEAproject1\\MovieRecommendSystem\\recommender\\dataloader\\src\\main\\resources\\movies.csv"
  val RATING_DATA_PATH = "C:\\IDEAproject1\\MovieRecommendSystem\\recommender\\dataloader\\src\\main\\resources\\ratings.csv"
  val TAG_DATA_PATH = "C:\\IDEAproject1\\MovieRecommendSystem\\recommender\\dataloader\\src\\main\\resources\\tags.csv"


  def main(args: Array[String]): Unit = {
    val config=Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://linux:27017/recommender",
      "mongo.db" -> "recommender",
      "es.httpHosts" -> "linux:9200",
      "es.transportHosts" -> "linux:9300",
      "es.index" -> ES_INDEX,
      "es.cluster.name" -> "es-cluster"
    )

    val sparkConf=new SparkConf().setAppName("DataLoader").setMaster(config.get("spark.cores").get)
    val spark=SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._
    val movieRDD=spark.sparkContext.textFile(MOVIE_DATA_PATH)
    val movieDF = movieRDD.map(
      item => {
        val attr = item.split("\\^")
        Movie(attr(0).toInt, attr(1).trim, attr(2).trim, attr(3).trim, attr(4).trim, attr(5).trim, attr(6).trim, attr(7).trim, attr(8).trim, attr(9).trim)
      }
    ).toDF()

    val ratingRDD=spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF = ratingRDD.map(item => {
      val attr = item.split(",")
      MovieRating(attr(0).toInt,attr(1).toInt,attr(2).toDouble,attr(3).toInt)
    }).toDF()

    val tagRDD=spark.sparkContext.textFile(TAG_DATA_PATH)
    val tagDF = tagRDD.map(item => {
      val attr = item.split(",")
      Tag(attr(0).toInt,attr(1).toInt,attr(2).trim,attr(3).toInt)
    }).toDF()

    implicit val mongoConfig = MongoConfig(config.get("mongo.uri").get, config.get("mongo.db").get)
    storeDataInMongoDB(movieDF,ratingDF,tagDF)

    import org.apache.spark.sql.functions._
    val newTag = tagDF.groupBy($"mid")
      .agg(concat_ws("|", collect_set($"tag"))
        .as("tags") )
      .select("mid", "tags")
    val movieWithTagsDF = movieDF.join(newTag, Seq("mid","mid"), "left")
    implicit val esConfig = ESConfig(config.get("es.httpHosts").get, config.get("es.transportHosts").get, config.get("es.index").get, config.get("es.cluster.name").get)
    storeDataInES(movieWithTagsDF)
    spark.stop()
  }//主程序结束位置

  def storeDataInMongoDB(movieDF: DataFrame,ratingDF :DataFrame,tagDF :DataFrame)(implicit mongoConfig:MongoConfig): Unit ={
    // 新建一个mongodb的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    // 如果mongodb中已经有相应的数据库，先删除
    mongoClient(mongoConfig.db)(MONGO_MOVIE_COLLECTION).dropCollection()
    mongoClient(mongoConfig.db)(MONGO_RATING_COLLECTION).dropCollection()
    mongoClient(mongoConfig.db)(MONGO_TAG_COLLECTION).dropCollection()

    // 将DF数据写入对应的mongodb表中
    movieDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGO_MOVIE_COLLECTION)
      .mode("overwrite")
      .format(MONGO_DRIVER_CLASS)
      .save()

    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGO_RATING_COLLECTION)
      .mode("overwrite")
      .format(MONGO_DRIVER_CLASS)
      .save()

    tagDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGO_TAG_COLLECTION)
      .mode("overwrite")
      .format(MONGO_DRIVER_CLASS)
      .save()

    //对数据表建索引
    mongoClient(mongoConfig.db)(MONGO_MOVIE_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(mongoConfig.db)(MONGO_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGO_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(mongoConfig.db)(MONGO_TAG_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGO_TAG_COLLECTION).createIndex(MongoDBObject("mid" -> 1))

    mongoClient.close()
  }

  def storeDataInES(movieDF:DataFrame)(implicit eSConfig: ESConfig): Unit ={
    //新建一个配置
    val settings:Settings = Settings.builder().put("cluster.name",eSConfig.clustername).build()

    val esClient = new PreBuiltTransportClient(settings)

    val REGEX_HOST_PORT = "(.+):(\\d+)".r
    eSConfig.transportHosts.split(",").foreach{
      case REGEX_HOST_PORT(host: String, port: String) => {
        esClient.addTransportAddress(new InetSocketTransportAddress( InetAddress.getByName(host), port.toInt ))
      }
    }
    if( esClient.admin().indices().exists( new IndicesExistsRequest(eSConfig.index) ).actionGet().isExists){
      esClient.admin().indices().delete( new DeleteIndexRequest(eSConfig.index) )
    }

    esClient.admin().indices().create( new CreateIndexRequest(eSConfig.index) )

    movieDF.write
      .option("es.nodes", eSConfig.httpHosts)
      .option("es.http.timeout", "100m")
      .option("es.mapping.id", "mid")
      .mode("overwrite")
      .format(ES_DRIVER_CLASS)
      .save(eSConfig.index + "/" + ES_TYPE)

  }
}
