package com.ridelink.ride_service.dto;

import com.ridelink.ride_service.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSummaryDto {

    private String id;
    private String userId;
    private String driverName;
    private String phoneNumber;
    private Double rating;
    private Integer totalTrips;
    private VehicleSummary vehicle;
    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleSummary {
        private String make;
        private String model;
        private Integer year;
        private String licensePlate;
        private VehicleType vehicleType;
        private Integer capacity;
    }
}
