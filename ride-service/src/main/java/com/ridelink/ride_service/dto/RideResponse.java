package com.ridelink.ride_service.dto;

import com.ridelink.ride_service.entity.LocationPoint;
import com.ridelink.ride_service.entity.RideStatus;
import com.ridelink.ride_service.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {

    private String id;
    private String passengerId;
    private String passengerName;
    private String passengerPhone;

    private String driverId;
    private String driverUserId;
    private String driverName;
    private String driverPhone;
    private String vehiclePlate;
    private String vehicleModel;

    private LocationPoint pickupLocation;
    private LocationPoint dropoffLocation;
    private VehicleType vehicleType;
    private Double estimatedDistanceKm;

    private RideStatus status;
    private Double fareAmount;
    private String cancellationReason;

    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
