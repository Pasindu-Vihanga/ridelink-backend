package com.ridelink.fare_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_fare_test_db",
    "services.ride-service.url=http://localhost:8083"
})
class FareServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
