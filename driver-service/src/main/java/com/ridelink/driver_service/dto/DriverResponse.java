package com.ridelink.driver_service.dto;

import com.ridelink.driver_service.entity.AvailabilityStatus;
import com.ridelink.driver_service.entity.Driver;
import com.ridelink.driver_service.entity.Location;
import com.ridelink.driver_service.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse {

    private String id;
    private String userId;
    private String driverName;
    private String phoneNumber;
    private String licenseNumber;
    private int experienceYears;
    private double rating;
    private int totalTrips;
    private Vehicle vehicle;
    private Location location;
    private AvailabilityStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DriverResponse fromDriver(Driver driver) {
        if (driver == null) {
            return null;
        }
        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .driverName(driver.getDriverName())
                .phoneNumber(driver.getPhoneNumber())
                .licenseNumber(driver.getLicenseNumber())
                .experienceYears(driver.getExperienceYears())
                .rating(driver.getRating())
                .totalTrips(driver.getTotalTrips())
                .vehicle(driver.getVehicle())
                .location(driver.getLocation())
                .status(driver.getStatus())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
