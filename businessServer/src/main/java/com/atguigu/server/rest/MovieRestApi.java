package com.atguigu.server.rest;

import org.springframework.ui.Model;



//用于处理电影相关的功能
public class MovieRestApi {

    //*******首页********************

    //提供获取实时推荐信息的接口
    public Model getRealtimeRecommendations(String username,Model model){

        return null;
    }

    //提供获取离线推荐信息的接口
    public Model getOfflineRecommendations(String username,Model model){

        return null;
    }

    //提供获取热门推荐信息的接口
    public Model getHotRecommendations(Model model){

        return null;
    }

    //提供获取优质电影的信息的接口
    public Model getRateMoreRecommendation(Model model){

        return  null;
    }

    //获取最新电影的信息的接口
    public Model getNewRecommendations(Model model){

        return null;
    }

    //*******模糊检索********************

    //提供基于名称或者描述的模糊检索功能
    public Model getFuzzySearchMovies(String query,Model model){

        return null;
    }

    //*******电影的详细页面***********************

    //获取单个电影的信息
    public Model getMovieInfo(int mid,Model model){

        return null;
    }

    //需要提供能够给电影打标签的功能
    public Model addTagToMovie(int mid,String  tagname,Model model){

        return null;
    }

    //获取某个电影的所有标签信息
    public Model getMovieTags(int mid,Model model){

        return null;
    }

    //获取电影的相似电影推荐
    public Model getSimMoviesRecommendation(int mid,Model model){

        return null;
    }

    //提供给电影打分的功能
    public Model rateMovie(int mid,Double score,Model model){

        return  null;
    }

    //*************电影的类别页面******************

    //需要能够提供影片类别的查找
    public Model getGenresMovies(String genres,Model model){

        return null;
    }

    //*************用户空间页面*********************

    //需要提供用户的所有电影评分的记录
    public Model getUserRatings(String username,Model model){

        return null;
    }

    //需要获取图表数据
    public Model getUserChart(String username,Model model){

        return null;
    }


}
