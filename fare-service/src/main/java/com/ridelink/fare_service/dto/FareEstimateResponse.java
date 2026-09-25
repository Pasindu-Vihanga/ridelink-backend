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
public class FareEstimateResponse {

    private VehicleType vehicleType;
    private Double distanceKm;
    private Double estimatedDurationMinutes;
    private Double baseFare;
    private Double distanceCharge;
    private Double timeCharge;
    private Double surgeMultiplier;
    private Double estimatedFare;
    @Builder.Default
    private String currency = "LKR";
    private String calculationFormula;
}
