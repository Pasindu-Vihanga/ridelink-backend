package com.ridelink.driver_service.dto;

import com.ridelink.driver_service.entity.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleRequest {

    @NotBlank(message = "Vehicle make is required")
    private String make;

    @NotBlank(message = "Vehicle model is required")
    private String model;

    @Min(value = 2000, message = "Vehicle year must be 2000 or newer")
    private int year;

    @NotBlank(message = "License plate number is required")
    private String licensePlate;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @Min(value = 1, message = "Capacity must be at least 1 passenger")
    @Max(value = 15, message = "Capacity cannot exceed 15 passengers")
    private int capacity;
}
