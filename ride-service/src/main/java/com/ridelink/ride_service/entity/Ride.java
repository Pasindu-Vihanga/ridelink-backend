package com.ridelink.ride_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rides")
public class Ride {

    @Id
    private String id;

    // Passenger references (Account Service)
    private String passengerId;
    private String passengerName;
    private String passengerPhone;

    // Driver references (Driver Service & Account Service)
    private String driverId;
    private String driverUserId;
    private String driverName;
    private String driverPhone;
    private String vehiclePlate;
    private String vehicleModel;

    // Journey details
    private LocationPoint pickupLocation;
    private LocationPoint dropoffLocation;
    private VehicleType vehicleType;
    private Double estimatedDistanceKm;

    // Lifecycle
    private RideStatus status;
    private Double fareAmount;
    private String cancellationReason;

    // Timestamps
    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
