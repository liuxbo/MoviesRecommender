package com.atguigu.server.service;


import com.atguigu.server.model.recom.Recommendation;
import com.atguigu.server.model.request.*;
import com.atguigu.server.utils.Constant;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//用于推荐服务
@Service
public class RecommenderService {

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private TransportClient esClient;

    private MongoDatabase mongoDatabase;

    private  MongoDatabase getMongoDatabase(){
        if(null == mongoDatabase)
            this.mongoDatabase = mongoClient.getDatabase(Constant.MONGO_DATABASE);
        return this.mongoDatabase;
    }

    /**
     * 获取混合推荐结果【用在当前电影的相似中】
     * @param request
     * @return
     */
    public List<Recommendation> getHybridRecommendations(GetHybridRecommendationRequest request){

        //获得电影相似矩阵中的结果
        List<Recommendation> itemCF = getItemCFMovies(new GetItemCFMoviesRequest(request.getMid(),request.getNum()));

        //获取基于内容推荐结果
        List<Recommendation> contentBased = getContentBasedRecommendations(new GetContentBasedRecommendationRequest(request.getMid(),request.getNum()));

        //返回结果
        List<Recommendation> result = new ArrayList<>();
        result.addAll(itemCF.subList(0,(int)Math.round(itemCF.size()*request.getCfShare())));
        result.addAll(contentBased.subList(0,(int)Math.round(contentBased.size()*(1 - request.getCfShare()))));
        return result;
    }


    public List<Recommendation> getItemCFMovies(GetItemCFMoviesRequest request){
        MongoCollection<Document> itemCFCollection = getMongoDatabase().getCollection(Constant.MONGO_MOVIE_RECS_COLLECTION);
        Document document = itemCFCollection.find(new Document("mid",request.getMid())).first();
        return parseDocument(document,request.getNum());
    }


    /**
     * 获取基于内容的推荐结果
     * @param request
     * @return
     */
    public List<Recommendation> getContentBasedRecommendations(GetContentBasedRecommendationRequest request){
        MoreLikeThisQueryBuilder queryBuilder = QueryBuilders.moreLikeThisQuery(new MoreLikeThisQueryBuilder.Item[]{
                new MoreLikeThisQueryBuilder.Item(Constant.ES_INDEX,Constant.ES_TYPE, String.valueOf(request.getMid()) )
        });

        SearchResponse response = esClient.prepareSearch(Constant.ES_INDEX).setQuery(queryBuilder).setSize(request.getSum()).execute().actionGet();

        return parseESResponse(response);

    }
    //用于解析ES的查询响应
    private List<Recommendation> parseESResponse(SearchResponse response) {
        List<Recommendation> recommendations = new ArrayList<>();
        for(SearchHit hit : response.getHits()){
           Map<String,Object> hitContents = hit.getSourceAsMap();
           recommendations.add(new Recommendation((int)hitContents.get("mid"),0D));
        }

        return recommendations;
    }



    /**
     * 用于获取ALS算法中用户推荐矩阵
     * @param request
     * @return
     */
    public List<Recommendation> getUserCFMovies(GetUserCFRequest request){
        MongoCollection<Document> userCFCollection = getMongoDatabase().getCollection(Constant.MONGO_USER_RECS_COLLECTION);
        Document document = userCFCollection.find(new Document("uid",request.getUid())).first();
        return parseDocument(document,request.getSum());
    }

    //用于解析Document
    private List<Recommendation> parseDocument(Document document,int sum) {
        List<Recommendation> result = new ArrayList<>();
        if(document == null || document.isEmpty())
            return result;
        ArrayList<Document> documents = document.get("recs",ArrayList.class);
        for (Document item : documents){
            result.add(new Recommendation(item.getInteger("rid"),item.getDouble("r")));
        }
        return result.subList(0,result.size() > sum ? sum : result.size());
    }



    /**
     * 获取当前用户的实时推荐
     * @param request
     * @return
     */
    public List<Recommendation> getSteamRecsMovies(GetStreamRecsRequest request){
        MongoCollection<Document> streamRecsCollection = getMongoDatabase().getCollection(Constant.MONGO_STREAM_RECS_COLLECTION);
        Document document = streamRecsCollection.find(new Document("uid",request.getUid())).first();

        //将推荐结果从数据库取出来
        List<Recommendation> result = new ArrayList<>();
        if(null == document || document.isEmpty())
            return result;
        for(String item:document.getString("recs").split("\\|")){
            String[] para = item.split(":");
            result.add(new Recommendation(Integer.parseInt(para[0]),Double.parseDouble(para[1])));
        }

        return result.subList(0,result.size() > request.getNum()?request.getNum():result.size());
    }


    /**
     * 获取电影类别的Top电影，用于处理冷启动问题
     * @param request
     * @return
     */
    public List<Recommendation> getGenresTopMovies(GetGenresTopMoviesRequest request){
      Document genresDocument =  getMongoDatabase().getCollection(Constant.MONGO_GENRES_TOP_MOVIES).find(new Document("genres",request.getGenres())).first();
      List<Recommendation> recommendations = new ArrayList<>();
      if(genresDocument == null || genresDocument.isEmpty())
          return recommendations;
      return parseDocument(genresDocument,request.getNum());
    }


    /**
     * 获取最热门电影
     * @param request
     * @return
     */
    public List<Recommendation> getHotRecommendations(GetHotRecommendationRequest request){
       FindIterable<Document> documents =  getMongoDatabase().getCollection(Constant.MONGO_RATE_MORE_RECENTLY_MOVIES).find().sort(Sorts.descending("yeahmouth"));
       List<Recommendation> recommendations = new ArrayList<>();
       for(Document item : documents){
           recommendations.add(new Recommendation(item.getInteger("mid"),0D));
       }
       return  recommendations.subList(0,recommendations.size() > request.getSum() ? request.getSum() : recommendations.size());

    }


    /**
     * 获取优质电影
     * @param request
     * @return
     */
    public List<Recommendation> getRateMoreMovies(GetRateMoreMoviesRequest request){
        FindIterable<Document> documents =  getMongoDatabase().getCollection(Constant.MONGO_RATE_MORE_MOVIES).find().sort(Sorts.descending("count"));
        List<Recommendation> recommendations = new ArrayList<>();
        for(Document item : documents){
            recommendations.add(new Recommendation(item.getInteger("mid"),0D));
        }
        return  recommendations.subList(0,recommendations.size() > request.getNum() ? request.getNum() : recommendations.size());

    }



    /**
     * 获取新电影
     * @param request
     * @return
     */
    public List<Recommendation> getNewMovies(GetNewMoviesRequest request){
        FindIterable<Document> documents =  getMongoDatabase().getCollection(Constant.MONGO_MOVIE_COLLECTION).find().sort(Sorts.descending("issue"));
        List<Recommendation> recommendations = new ArrayList<>();

        for(Document item : documents){
            recommendations.add(new Recommendation(item.getInteger("mid"),0D));
        }
        return  recommendations.subList(0,recommendations.size() > request.getNum() ? request.getNum() : recommendations.size());

    }


    /**
     * 模糊检索主题
     * @param request
     * @return
     */
    public List<Recommendation> getFuzzyMovies(GetFuzzySearchMoviesRequest request){
        FuzzyQueryBuilder queryBuilder = QueryBuilders.fuzzyQuery("name",request.getQuery());
        SearchResponse searchResponse = esClient.prepareSearch(Constant.ES_INDEX).setQuery(queryBuilder).setSize(request.getNum()).execute().actionGet();
        return parseESResponse(searchResponse);
    }


    public List<Recommendation> getGenresMovies(GetGenresMoviesRequest request){
        FuzzyQueryBuilder queryBuilder = QueryBuilders.fuzzyQuery("genres",request.getGenres());
        SearchResponse searchResponse = esClient.prepareSearch(Constant.ES_INDEX).setQuery(queryBuilder).setSize(request.getNum()).execute().actionGet();
        return parseESResponse(searchResponse);
    }

}
