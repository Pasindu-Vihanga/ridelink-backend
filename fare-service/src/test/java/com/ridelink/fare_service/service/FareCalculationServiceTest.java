package com.ridelink.fare_service.service;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.entity.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FareCalculationServiceTest {

    private FareCalculationService fareCalculationService;

    @BeforeEach
    void setUp() {
        fareCalculationService = new FareCalculationService();
    }

    @Test
    void calculateEstimate_SingleVehicleType_Car() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(10.0)
                .estimatedDurationMinutes(20.0)
                .vehicleType(VehicleType.CAR)
                .build();

        List<FareEstimateResponse> estimates = fareCalculationService.calculateEstimate(request);

        assertEquals(1, estimates.size());
        FareEstimateResponse carEstimate = estimates.get(0);

        assertEquals(VehicleType.CAR, carEstimate.getVehicleType());
        assertEquals(200.0, carEstimate.getBaseFare());
        assertEquals(1200.0, carEstimate.getDistanceCharge()); // 10 km * 120
        assertEquals(200.0, carEstimate.getTimeCharge());      // 20 min * 10
        assertEquals(1600.0, carEstimate.getEstimatedFare());  // 200 + 1200 + 200 = 1600
        assertNotNull(carEstimate.getCalculationFormula());
    }

    @Test
    void calculateEstimate_AllVehicleTypes_WhenVehicleTypeNull() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(5.0)
                .build();

        List<FareEstimateResponse> estimates = fareCalculationService.calculateEstimate(request);

        assertEquals(4, estimates.size()); // CAR, VAN, BIKE, TUK
    }

    @Test
    void calculateEstimate_EnforcesMinimumFareFloor() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(0.1) // very short trip
                .estimatedDurationMinutes(1.0)
                .vehicleType(VehicleType.VAN)
                .build();

        List<FareEstimateResponse> estimates = fareCalculationService.calculateEstimate(request);

        // VAN min fare is 400.0
        assertTrue(estimates.get(0).getEstimatedFare() >= 400.0);
    }

    @Test
    void calculateFinalFare_ComputesAccurateBreakdown() {
        FareCalculationService.FinalFareCalculation result =
                fareCalculationService.calculateFinalFare(VehicleType.TUK, 4.0, 10.0);

        // TUK: Base 100, PerKm 80 * 4 = 320, PerMin 8 * 10 = 80 -> Total = 500.0
        assertEquals(100.0, result.baseFare());
        assertEquals(320.0, result.distanceCharge());
        assertEquals(80.0, result.timeCharge());
        assertEquals(500.0, result.totalFare());
    }
}
