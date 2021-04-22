package com.atguigu.server.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import redis.clients.jedis.Jedis;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//通过Configure类来实例化Bean
@Configuration

public class Configure {

    private String jedisHost;
    private String mongoHost;
    private int mongoPort;
    private  String esClusterName;
    private String esHost;
    private String esPort;

    public Configure() throws IOException {
        //加载配置文件
        Properties properties = new Properties();
        Resource resource = new ClassPathResource("application.properties");
        properties.load(new FileInputStream(resource.getFile()));

        //具体加载了配置文件
        this.jedisHost = properties.getProperty("jedis.host");
        this.mongoHost = properties.getProperty()
    }

    //将jedis注册为一个Bean
    @Bean("jedis")
    public Jedis getJedis(){
        Jedis jedis = new Jedis("host");
        return jedis;
    }




}
