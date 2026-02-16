package com.innowise.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MongoConfig {

    @Bean
    @Primary
    public MongoClient mongoClient(@Value("${spring.data.mongodb.uri}") String uri) {
        return MongoClients.create(uri);
    }
}