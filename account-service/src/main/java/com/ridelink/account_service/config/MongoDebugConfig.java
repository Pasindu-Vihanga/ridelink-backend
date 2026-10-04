package com.ridelink.account_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoDebugConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("=== MongoDB Connection Debug ===");
            
            // Check connection and database
            String dbName = mongoTemplate.getDb().getName();
            System.out.println("✅ Connected to Database: " + dbName);
            
            // Check collections
            System.out.println("📋 Collections in database:");
            for (String collectionName : mongoTemplate.getDb().listCollectionNames()) {
                System.out.println("  - " + collectionName);
            }
            
            System.out.println("🎯 Database should be visible in Atlas as: " + dbName);
            
        } catch (Exception e) {
            System.err.println("❌ MongoDB Connection Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}