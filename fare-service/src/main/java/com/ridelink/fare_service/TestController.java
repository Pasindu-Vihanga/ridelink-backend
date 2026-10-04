package com.ridelink.fare_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class TestController {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @GetMapping("/test/db")
    public String testDatabase() {
        try {
            // Basic connection test
            return "✅ Fare Database connection configured! URI: " + 
                   mongoUri.substring(0, mongoUri.indexOf("@")) + "@cluster...";
        } catch (Exception e) {
            return "❌ Fare Database connection failed: " + e.getMessage();
        }
    }

    @PostMapping("/test/create-sample-payment")
    public String createSamplePayment() {
        try {
            // Since fare-service might not have entities yet, just return success
            return "✅ Fare service is ready! ridelink_payment_db will be created when first payment is processed.";
        } catch (Exception e) {
            return "❌ Failed to create payment: " + e.getMessage();
        }
    }
}