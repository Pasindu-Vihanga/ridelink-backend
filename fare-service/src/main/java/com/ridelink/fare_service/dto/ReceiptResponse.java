package com.ridelink.fare_service.dto;

import com.ridelink.fare_service.entity.PaymentMethod;
import com.ridelink.fare_service.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private String id;
    private String receiptNumber;
    private String transactionId;
    private String paymentId;
    private String rideId;
    private String passengerId;
    private String passengerName;
    private String driverName;
    private String vehiclePlate;
    private VehicleType vehicleType;
    private Double distanceKm;
    private Double durationMinutes;

    // Itemized charges
    private Double baseFare;
    private Double distanceCharge;
    private Double timeCharge;
    private Double surgeMultiplier;
    private Double totalAmount;
    @Builder.Default
    private String currency = "LKR";

    private PaymentMethod paymentMethod;
    private LocalDateTime issuedAt;
}
