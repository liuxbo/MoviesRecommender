package com.atguigu.server.service;

//具体处理业务服务的服务类

import com.atguigu.java.model.Constant;
import com.atguigu.server.model.core.User;
import com.atguigu.server.model.request.LoginUserRequest;
import com.atguigu.server.model.request.RegisterUserRequest;
import com.atguigu.server.model.request.UpdateUserGenresRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UserService {

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ObjectMapper objectMapper;
    private MongoCollection<Document> userCollection;

    //获取user表连接
    private MongoCollection<Document> getUserCollection(){
        if(userCollection == null)
            this.userCollection = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_USER_COLLECTION);

        return this.userCollection;
    }

    //将user转换成一个Document
    private Document userToDocument(User user){
        try {
            Document document = Document.parse(objectMapper.writeValueAsString(user));
            return document;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    //将Document转换成User
    private User documentToUser(Document document){
        try {
            User user = objectMapper.readValue(JSON.serialize(document),User.class);
            return user;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 注册用户服务
     * @param request
     * @return
     */
    public boolean registerUser(RegisterUserRequest request){
        //判断是否有相同的用户已经注册过
        if(getUserCollection().find(new Document("username",request.getUsername())).first() != null)
            return false;

        //创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFirst(true);


        //插入用户
        Document document = userToDocument(user);
        if(document == null)
            return false;
        getUserCollection().insertOne(document);
        return true;
    }

    /**
     * 登录
     * @param request
     * @return
     */
    public boolean loginUser(LoginUserRequest request){
        //找到该用户
        Document  document = getUserCollection().find(new Document("username",request.getUsername())).first();
        if(null == document)
            return false;
        User user = documentToUser(document);

        //验证密码
        if(null == user)
            return false;
        return user.getPassword().compareTo(request.getPassword()) == 0;

    }

    /**
     * 更新用户第一次登陆选择的电影类别
     * @param request
     * @return
     */
    public void updateUserGenres(UpdateUserGenresRequest request){
        getUserCollection().updateOne(new Document("username",request.getUsername()),new Document().append("$set",new Document("$genres",request.getGenres())));
        getUserCollection().updateOne(new Document("username",request.getUsername()),new Document().append("$set",new Document("$first",false)));
    }

    //通过用户名查询一个用户
    public User findUserByUsername(String username){
        Document document = getUserCollection().find(new Document("username",username)).first();
        if(null == document || document.isEmpty())
            return null;
        return documentToUser(document);
    }

}
