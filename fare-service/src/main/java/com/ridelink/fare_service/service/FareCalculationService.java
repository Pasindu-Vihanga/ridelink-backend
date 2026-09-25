package com.ridelink.fare_service.service;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.entity.VehicleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class FareCalculationService {

    public record RateCard(double baseFare, double perKmRate, double perMinuteRate, double minimumFare) {}

    public static RateCard getRateCard(VehicleType vehicleType) {
        return switch (vehicleType) {
            case CAR -> new RateCard(200.0, 120.0, 10.0, 250.0);
            case VAN -> new RateCard(350.0, 160.0, 15.0, 400.0);
            case TUK -> new RateCard(100.0, 80.0, 8.0, 150.0);
            case BIKE -> new RateCard(80.0, 50.0, 5.0, 100.0);
        };
    }

    /**
     * Calculate fare estimates for a request.
     * If vehicleType is specified, returns estimate for that type.
     * If vehicleType is null, returns estimates for all vehicle types.
     */
    public List<FareEstimateResponse> calculateEstimate(FareEstimateRequest request) {
        double distanceKm = request.getDistanceKm();
        // If duration is not provided, estimate based on average city speed of 25 km/h: (distance / 25) * 60 minutes
        double durationMinutes = request.getEstimatedDurationMinutes() != null && request.getEstimatedDurationMinutes() > 0
                ? request.getEstimatedDurationMinutes()
                : Math.max(5.0, Math.round((distanceKm / 25.0) * 60.0));

        List<FareEstimateResponse> estimates = new ArrayList<>();

        if (request.getVehicleType() != null) {
            estimates.add(computeEstimate(request.getVehicleType(), distanceKm, durationMinutes));
        } else {
            for (VehicleType type : VehicleType.values()) {
                estimates.add(computeEstimate(type, distanceKm, durationMinutes));
            }
        }

        return estimates;
    }

    public FareEstimateResponse computeEstimate(VehicleType vehicleType, double distanceKm, double durationMinutes) {
        RateCard rates = getRateCard(vehicleType);
        double distanceCharge = round(rates.perKmRate() * distanceKm);
        double timeCharge = round(rates.perMinuteRate() * durationMinutes);
        double rawTotal = rates.baseFare() + distanceCharge + timeCharge;
        double surgeMultiplier = 1.0; // Standard rate multiplier

        double finalFare = Math.max(rates.minimumFare(), round(rawTotal * surgeMultiplier));

        String formula = String.format("max(%.2f, round((%.2f + (%.2f * %.2f) + (%.2f * %.2f)) * %.1f, 2))",
                rates.minimumFare(), rates.baseFare(), rates.perKmRate(), distanceKm, rates.perMinuteRate(), durationMinutes, surgeMultiplier);

        return FareEstimateResponse.builder()
                .vehicleType(vehicleType)
                .distanceKm(distanceKm)
                .estimatedDurationMinutes(durationMinutes)
                .baseFare(rates.baseFare())
                .distanceCharge(distanceCharge)
                .timeCharge(timeCharge)
                .surgeMultiplier(surgeMultiplier)
                .estimatedFare(finalFare)
                .currency("LKR")
                .calculationFormula(formula)
                .build();
    }

    public record FinalFareCalculation(
            double baseFare,
            double distanceCharge,
            double timeCharge,
            double surgeMultiplier,
            double totalFare,
            double durationMinutes
    ) {}

    public FinalFareCalculation calculateFinalFare(VehicleType vehicleType, double distanceKm, Double durationMinutes) {
        RateCard rates = getRateCard(vehicleType);
        double actualDuration = durationMinutes != null && durationMinutes > 0
                ? durationMinutes
                : Math.max(5.0, Math.round((distanceKm / 25.0) * 60.0));

        double distanceCharge = round(rates.perKmRate() * distanceKm);
        double timeCharge = round(rates.perMinuteRate() * actualDuration);
        double rawTotal = rates.baseFare() + distanceCharge + timeCharge;
        double surgeMultiplier = 1.0;

        double finalFare = Math.max(rates.minimumFare(), round(rawTotal * surgeMultiplier));

        return new FinalFareCalculation(
                rates.baseFare(),
                distanceCharge,
                timeCharge,
                surgeMultiplier,
                finalFare,
                actualDuration
        );
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
