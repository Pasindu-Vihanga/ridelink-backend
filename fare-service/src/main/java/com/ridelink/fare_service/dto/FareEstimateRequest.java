package com.ridelink.fare_service.dto;

import com.ridelink.fare_service.entity.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareEstimateRequest {

    private String pickupAddress;
    private String dropoffAddress;

    @NotNull(message = "Distance in km is required")
    @Positive(message = "Distance must be positive")
    private Double distanceKm;

    private Double estimatedDurationMinutes;

    private VehicleType vehicleType;
}
