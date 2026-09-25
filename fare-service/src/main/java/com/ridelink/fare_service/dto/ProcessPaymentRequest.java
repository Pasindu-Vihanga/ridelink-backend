package com.ridelink.fare_service.dto;

import com.ridelink.fare_service.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    @NotBlank(message = "Passenger ID is required")
    private String passengerId;

    @NotNull(message = "Payment method is required (CARD, CASH, WALLET)")
    private PaymentMethod paymentMethod;

    private Double actualDistanceKm;

    private Double actualDurationMinutes;

    /**
     * Optional testing flag to simulate failure scenarios (declined card, insufficient funds)
     * as required by Assignment Workflow 7 & Rubric G2.
     */
    @Builder.Default
    private Boolean simulateFailure = false;
}
