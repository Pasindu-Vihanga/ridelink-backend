package com.ridelink.driver_service.controller;

import com.ridelink.driver_service.dto.*;
import com.ridelink.driver_service.entity.VehicleType;
import com.ridelink.driver_service.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver & Vehicle Management", description = "Operations for driver operational profiles, vehicles, availability, and location")
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    @Operation(summary = "Register driver profile and vehicle", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Driver profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or Account Service verification failure"),
            @ApiResponse(responseCode = "409", description = "Driver profile or license number already exists")
    })
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody DriverRegistrationRequest request) {
        DriverResponse response = driverService.registerDriver(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver profile by Driver ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable String id) {
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get driver profile by Account Service User ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver profile found"),
            @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    public ResponseEntity<DriverResponse> getDriverByUserId(@PathVariable String userId) {
        DriverResponse response = driverService.getDriverByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/vehicle")
    @Operation(summary = "Update driver vehicle details", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> updateVehicle(
            @PathVariable String id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        DriverResponse response = driverService.updateVehicle(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Update driver availability status (ONLINE, OFFLINE, ON_TRIP)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability updated successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> updateAvailability(
            @PathVariable String id,
            @Valid @RequestBody UpdateAvailabilityRequest request
    ) {
        DriverResponse response = driverService.updateAvailability(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/location")
    @Operation(summary = "Update driver simulated location and service area", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location updated successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> updateLocation(
            @PathVariable String id,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        DriverResponse response = driverService.updateLocation(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/trip-status")
    @Operation(summary = "Update driver trip assignment status (Used by Ride Management Service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trip status updated"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> updateTripStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateTripStatusRequest request
    ) {
        DriverResponse response = driverService.updateTripStatus(id, request.getOnTrip());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "Retrieve eligible available drivers (Used by Ride Management Service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of available eligible drivers")
    })
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) VehicleType vehicleType
    ) {
        List<DriverResponse> response = driverService.getAvailableDrivers(serviceArea, vehicleType);
        return ResponseEntity.ok(response);
    }
}
