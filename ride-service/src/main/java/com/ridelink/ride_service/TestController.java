package com.ridelink.ride_service;

import com.ridelink.ride_service.entity.Ride;
import com.ridelink.ride_service.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class TestController {

    @Autowired
    private RideRepository rideRepository;

    @GetMapping("/test/db")
    public String testDatabase() {
        try {
            long count = rideRepository.count();
            return "✅ Ride Database connection successful! Ride count: " + count;
        } catch (Exception e) {
            return "❌ Ride Database connection failed: " + e.getMessage();
        }
    }

    @PostMapping("/test/create-sample-ride")
    public String createSampleRide() {
        try {
            if (rideRepository.count() > 0) {
                return "✅ Sample ride already exists in database!";
            }

            // Note: This creates a minimal ride for DB testing only
            // Real rides need proper validation and references
            Ride sampleRide = Ride.builder()
                    .passengerId("sample-passenger-id")
                    .driverId("sample-driver-id")
                    .pickupLocation(com.ridelink.ride_service.entity.LocationPoint.builder().address("Sample Pickup Location").build())
                    .dropoffLocation(com.ridelink.ride_service.entity.LocationPoint.builder().address("Sample Dropoff Location").build())
                    .status(com.ridelink.ride_service.entity.RideStatus.COMPLETED)
                    .fareAmount(750.0)
                    .requestedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            Ride saved = rideRepository.save(sampleRide);
            return "✅ Sample ride created! ID: " + saved.getId() + " - ridelink_ride_db should now be visible!";
        } catch (Exception e) {
            return "❌ Failed to create ride: " + e.getMessage();
        }
    }
}