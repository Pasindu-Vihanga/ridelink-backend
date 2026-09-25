package com.ridelink.ride_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_ride_test_db",
    "services.account-service.url=http://localhost:8081",
    "services.driver-service.url=http://localhost:8082"
})
class RideServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
