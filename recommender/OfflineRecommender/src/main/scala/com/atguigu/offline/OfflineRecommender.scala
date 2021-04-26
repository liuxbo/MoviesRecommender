package com.atguigu.offline

import com.mongodb.Mongo
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix
import com.atguigu.java.model.Constant._
import com.atguigu.scala.model.{MongoConfig, Movie, MovieRating, MovieRecs, Recommendation, UserRecs}



object OfflineRecommender {

  val USER_MAX_RECOMMENDATION = 20

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://linux:27017/recommender",
      "mongo.db" -> "recommender"
    )
    //创建一个SparkConf配置
    val sparkConf = new SparkConf().setAppName("OfflineRecommender").setMaster(config("spark.cores"))
      .set("spark.executor.memory","6G").set("spark.driver.memory","2G")

    //基于SparkConf创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    //创建一个MongoDBConfig
    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    //读取mongodb中的业务数据
    val ratingRDD = spark.read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_RATING_COLLECTION)
      .format(MONGO_DRIVER_CLASS)
      .load()
      .as[MovieRating]
      .rdd
      .map(rating=>(rating.uid,rating.mid,rating.score)).cache()

    //用户数据集 RDD[Int]
    val userRDD= ratingRDD.map(_._1).distinct()

    //电影数据集 RDD[Int]
    val movieRDD = spark.read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_MOVIE_COLLECTION)
      .format(MONGO_DRIVER_CLASS)
      .load()
      .as[Movie]
      .rdd
      .map(_.mid).cache()

    //创建训练数据集
    val trainData = ratingRDD.map(x => Rating(x._1,x._2,x._3))
    val (rank,iterations,lambda) = (50,5,0.01)
    //训练ALS模型
    val model = ALS.train(trainData,rank,iterations,lambda)

    //计算用户推荐矩阵
      //需要构造一个usersProducts RDD[(Int,Int)]
   val userMovies = userRDD.cartesian(movieRDD)
    val preRatings = model.predict(userMovies)

    val userRecs = preRatings
      .filter(_.rating > 0)
      .map(rating => (rating.user,(rating.product, rating.rating)))
      .groupByKey()
      .map{
        case (uid,recs) => UserRecs(uid,recs.toList.sortWith(_._2 > _._2).take(USER_MAX_RECOMMENDATION).map(x => Recommendation(x._1,x._2)))
      }.toDF()

    userRecs.write
        .option("uri",mongoConfig.uri)
        .option("collection",MONGO_USER_RECS_COLLECTION)
        .mode("overwrite")
        .format(MONGO_DRIVER_CLASS)
        .save()

    //计算电影相似度矩阵
     //获取电影的特征矩阵
    val movieFeatures = model.productFeatures.map{case (mid,freatures) =>
       (mid,new DoubleMatrix(freatures))
     }
    val movieRecs = movieFeatures.cartesian(movieFeatures)
        .filter{case(a,b) => a._1 != b._1} //只要不相等的
        .map{ case(a,b) =>
          val simScore = this.consinSim(a._2,b._2)
          (a._1,(b._1,simScore))
        }.filter(_._2._2 > 0.6)
      .groupByKey()
        .map{case (mid,items) =>
          MovieRecs(mid,items.toList.map(x=>Recommendation(x._1,x._2)))
        }.toDF()
    movieRecs.write
        .option("uri",mongoConfig.uri)
        .option("collection",MONGO_MOVIE_RECS_COLLECTION)
        .mode("overwrite")
        .format(MONGO_DRIVER_CLASS)
        .save()

   spark.close()
  }//main方法结束

  //计算两个电影之间的余弦相似度
  def consinSim(movie1: DoubleMatrix , movie2:DoubleMatrix) : Double ={
    movie1.dot(movie2) / ( movie1.norm2() * movie2.norm2())

  }
}
