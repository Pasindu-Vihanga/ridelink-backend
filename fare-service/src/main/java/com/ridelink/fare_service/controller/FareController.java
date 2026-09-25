package com.ridelink.fare_service.controller;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.service.FareCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
@Tag(name = "Fare Estimation", description = "Endpoints for transparent fare estimation using documented pricing formula")
public class FareController {

    private final FareCalculationService fareCalculationService;

    @PostMapping("/estimate")
    @Operation(summary = "Calculate upfront fare estimate", description = "Calculates estimated fare based on vehicle type, distance, and duration using documented formula.")
    public ResponseEntity<List<FareEstimateResponse>> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareCalculationService.calculateEstimate(request));
    }
}
