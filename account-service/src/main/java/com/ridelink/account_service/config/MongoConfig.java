package com.ridelink.account_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.ridelink.account_service.repository")
public class MongoConfig {
    // Spring Boot auto-configuration will handle the rest
}