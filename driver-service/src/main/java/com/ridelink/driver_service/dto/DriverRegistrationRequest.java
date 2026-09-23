package com.ridelink.driver_service.dto;

import com.ridelink.driver_service.entity.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class DriverRegistrationRequest {

    @NotBlank(message = "User ID is required from Account Service")
    private String userId;

    @NotBlank(message = "Driver name is required")
    private String driverName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+0-9]{9,15}$", message = "Phone number must be a valid format")
    private String phoneNumber;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @Min(value = 1, message = "Experience must be at least 1 year")
    private int experienceYears;

    // Vehicle Info
    @NotBlank(message = "Vehicle make is required")
    private String vehicleMake;

    @NotBlank(message = "Vehicle model is required")
    private String vehicleModel;

    @Min(value = 2000, message = "Vehicle year must be 2000 or newer")
    private int vehicleYear;

    @NotBlank(message = "License plate number is required")
    private String licensePlate;

    @NotNull(message = "Vehicle type is required (CAR, VAN, BIKE, TUK)")
    private VehicleType vehicleType;

    @Min(value = 1, message = "Capacity must be at least 1 passenger")
    @Max(value = 15, message = "Capacity cannot exceed 15 passengers")
    private int vehicleCapacity;

    // Initial Location Info
    private double latitude;
    private double longitude;

    @NotBlank(message = "Service area is required (e.g. Colombo, Kandy)")
    private String serviceArea;
}
