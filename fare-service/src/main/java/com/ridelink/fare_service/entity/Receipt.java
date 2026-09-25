package com.ridelink.fare_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "receipts")
public class Receipt {

    @Id
    private String id;
    private String receiptNumber;
    private String paymentId;
    private String transactionId;
    private String rideId;
    private String passengerId;
    private String passengerName;
    private String driverName;
    private String vehiclePlate;
    private VehicleType vehicleType;
    private Double distanceKm;
    private Double durationMinutes;
    private Double baseFare;
    private Double distanceCharge;
    private Double timeCharge;
    private Double surgeMultiplier;
    private Double totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDateTime issuedAt;
}
