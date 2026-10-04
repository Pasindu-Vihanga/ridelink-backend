package com.ridelink.driver_service;

import com.ridelink.driver_service.entity.Driver;
import com.ridelink.driver_service.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class TestController {

    @Autowired
    private DriverRepository driverRepository;

    @GetMapping("/test/db")
    public String testDatabase() {
        try {
            long count = driverRepository.count();
            return "✅ Driver Database connection successful! Driver count: " + count;
        } catch (Exception e) {
            return "❌ Driver Database connection failed: " + e.getMessage();
        }
    }

    @PostMapping("/test/create-sample-driver")
    public String createSampleDriver() {
        try {
            if (driverRepository.count() > 0) {
                return "✅ Sample driver already exists in database!";
            }

            // Note: This creates a minimal driver for DB testing only
            // Real drivers need proper validation and user reference
            Driver sampleDriver = Driver.builder()
                    .userId("sample-user-id")
                    .licenseNumber("TEST-LICENSE-123")
                    .rating(5.0)
                    .totalTrips(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Driver saved = driverRepository.save(sampleDriver);
            return "✅ Sample driver created! ID: " + saved.getId() + " - ridelink_driver_db should now be visible!";
        } catch (Exception e) {
            return "❌ Failed to create driver: " + e.getMessage();
        }
    }
}