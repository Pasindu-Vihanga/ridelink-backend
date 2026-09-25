package com.ridelink.ride_service.controller;

import com.ridelink.ride_service.dto.CancelRideRequest;
import com.ridelink.ride_service.dto.CreateRideRequest;
import com.ridelink.ride_service.dto.RideResponse;
import com.ridelink.ride_service.entity.RideStatus;
import com.ridelink.ride_service.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Tag(name = "Ride Management", description = "Endpoints for ride requests, driver dispatch, and ride lifecycle management")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @Operation(summary = "Create ride request and auto-assign driver", description = "Validates passenger with Account Service, matches available drivers via Driver Service, and assigns best driver.")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideResponse response = rideService.createRide(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ride details by ID")
    public ResponseEntity<RideResponse> getRideById(@PathVariable String id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }

    @GetMapping("/passenger/{passengerId}")
    @Operation(summary = "Get ride history for a specific passenger")
    public ResponseEntity<List<RideResponse>> getRidesByPassengerId(@PathVariable String passengerId) {
        return ResponseEntity.ok(rideService.getRidesByPassengerId(passengerId));
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get ride history for a specific driver")
    public ResponseEntity<List<RideResponse>> getRidesByDriverId(@PathVariable String driverId) {
        return ResponseEntity.ok(rideService.getRidesByDriverId(driverId));
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Driver accepts an assigned ride", description = "Transitions status from ASSIGNED to ACCEPTED.")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable String id) {
        return ResponseEntity.ok(rideService.acceptRide(id));
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Driver starts the trip", description = "Transitions status from ACCEPTED to IN_PROGRESS.")
    public ResponseEntity<RideResponse> startRide(@PathVariable String id) {
        return ResponseEntity.ok(rideService.startRide(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Driver completes the trip", description = "Transitions status from IN_PROGRESS to COMPLETED and releases driver in Driver Service.")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String id) {
        return ResponseEntity.ok(rideService.completeRide(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a ride", description = "Transitions status to CANCELLED and releases assigned driver.")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable String id,
            @RequestBody(required = false) CancelRideRequest cancelRequest) {
        String reason = cancelRequest != null ? cancelRequest.getReason() : "Cancelled by user";
        return ResponseEntity.ok(rideService.cancelRide(id, reason));
    }

    @GetMapping
    @Operation(summary = "Get all rides with optional status filter")
    public ResponseEntity<List<RideResponse>> getAllRides(@RequestParam(required = false) RideStatus status) {
        return ResponseEntity.ok(rideService.getAllRides(status));
    }
}
