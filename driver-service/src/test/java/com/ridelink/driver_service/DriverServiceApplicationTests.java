package com.ridelink.driver_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_driver_test_db",
    "services.account-service.url=http://localhost:8081"
})
class DriverServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
