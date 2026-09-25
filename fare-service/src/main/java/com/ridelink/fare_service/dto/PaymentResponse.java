package com.ridelink.fare_service.dto;

import com.ridelink.fare_service.entity.PaymentMethod;
import com.ridelink.fare_service.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String id;
    private String transactionId;
    private String rideId;
    private String passengerId;
    private Double amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime paidAt;
    private String receiptNumber;
}
