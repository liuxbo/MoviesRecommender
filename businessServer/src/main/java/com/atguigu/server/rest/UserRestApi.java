package com.atguigu.server.rest;

import com.atguigu.server.model.core.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

//用于处理User相关的动作

@Controller
@RequestMapping("/rest/users")

public class UserRestApi {

    //需要提供用户注册功能  该方法路径为 /rest/users/register
    @RequestMapping(path = "/register",produces = "application/json",method = RequestMethod.GET)
    @ResponseBody
    public Model registerUser(String username,String password,Model model){
        User user  = new User();
        user.setUsername("ddddd");
        user.setPassword("fffff");
        user.getGenres().add("abc");
        user.getGenres().add("tyu");
        model.addAttribute("success","false");
        model.addAttribute("user",user);
        return null;
    }

    //登录功能
    @RequestMapping(path = "/login",produces = "application/json",method = RequestMethod.GET)
    @ResponseBody
    public User loginUser(String username, String password, Model model){
        User user  = new User();
        user.setUsername("ddddd");
        user.setPassword("fffff");
        user.getGenres().add("abc");
        user.getGenres().add("tyu");
        return user;
    }

    //需要能够添加用户偏爱的影片类别
    public Model addGenres(String username,String genres,Model model){

        return null;
    }
}
