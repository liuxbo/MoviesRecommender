package com.atguigu.scala.model

case class Movie(val mid: Int,val name: String,val descri: String,val timelong: String,val issue: String,
                 val  shoot: String,val language: String,val genres: String,val actors: String,val directors: String)
case class  MovieRating(val uid:Int,val mid:Int,val score:Double,val timestamp:Int)
case class Tag(val uid:Int,val mid:Int,val tag:String,val timestamp:Int)
case class MongoConfig(val uri:String,val db:String)
case class ESConfig(val httpHosts:String,val transportHosts:String,val index:String,val clustername:String)

case class Recommendation(rid:Int, r:Double)

// 用户的推荐
case class UserRecs(uid:Int, recs:Seq[Recommendation])

//电影的相似度
case class MovieRecs(mid:Int, recs:Seq[Recommendation])

/**
 * 电影类比的推荐
 * @param genres  电影的类别
 * @param recs     top10的电影的集合
 */
case class GenresRecommendation(genres:String,recs:Seq[Recommendation])



