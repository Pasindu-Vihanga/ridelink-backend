package com.ridelink.fare_service.dto;

import com.ridelink.fare_service.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideValidationDto {
    private String id;
    private String passengerId;
    private String passengerName;
    private String driverId;
    private String driverName;
    private String vehiclePlate;
    private VehicleType vehicleType;
    private Double estimatedDistanceKm;
    private String status;
    private Double fareAmount;
}
