package com.ridelink.fare_service.service;

import com.ridelink.fare_service.client.RideServiceClient;
import com.ridelink.fare_service.dto.*;
import com.ridelink.fare_service.entity.*;
import com.ridelink.fare_service.exception.DuplicatePaymentException;
import com.ridelink.fare_service.exception.PaymentFailedException;
import com.ridelink.fare_service.exception.ResourceNotFoundException;
import com.ridelink.fare_service.repository.PaymentRepository;
import com.ridelink.fare_service.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final RideServiceClient rideServiceClient;
    private final FareCalculationService fareCalculationService;

    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing simulated payment for ride ID: {} by passenger: {}", request.getRideId(), request.getPassengerId());

        // 1. Interservice Validation: Verify Ride with Ride Management Service
        RideValidationDto ride = rideServiceClient.getAndValidateRide(request.getRideId(), request.getPassengerId());
        log.info("Ride validated successfully: Status={}, VehicleType={}", ride.getStatus(), ride.getVehicleType());

        // 2. Prevent Duplicate Payment (Idempotency check)
        if (paymentRepository.existsByRideIdAndStatus(request.getRideId(), PaymentStatus.COMPLETED)) {
            throw new DuplicatePaymentException("Payment has already been successfully recorded for ride ID: " + request.getRideId());
        }

        // 3. Simulated Payment Failure (Negative Scenario support for Workflow 7 & Rubric G2)
        if (Boolean.TRUE.equals(request.getSimulateFailure())) {
            String txnId = generateTransactionId();
            Payment failedPayment = Payment.builder()
                    .transactionId(txnId)
                    .rideId(request.getRideId())
                    .passengerId(request.getPassengerId())
                    .amount(ride.getFareAmount() != null ? ride.getFareAmount() : 0.0)
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED)
                    .failureReason("Simulated card payment authorization declined by issuer (insufficient funds)")
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(failedPayment);
            log.warn("Payment failed for ride ID {}: Simulated decline", request.getRideId());
            throw new PaymentFailedException("Payment processing failed: Transaction declined by simulated card processor");
        }

        // 4. Calculate Final Fare using documented formula
        double distanceKm = request.getActualDistanceKm() != null && request.getActualDistanceKm() > 0
                ? request.getActualDistanceKm()
                : (ride.getEstimatedDistanceKm() != null ? ride.getEstimatedDistanceKm() : 5.0);

        VehicleType vehicleType = ride.getVehicleType() != null ? ride.getVehicleType() : VehicleType.CAR;

        FareCalculationService.FinalFareCalculation fareBreakdown = fareCalculationService.calculateFinalFare(
                vehicleType,
                distanceKm,
                request.getActualDurationMinutes()
        );

        // 5. Create and persist Payment record
        String transactionId = generateTransactionId();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .rideId(request.getRideId())
                .passengerId(request.getPassengerId())
                .amount(fareBreakdown.totalFare())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.COMPLETED)
                .paidAt(now)
                .createdAt(now)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment recorded successfully: ID={}, TransactionId={}, Amount=LKR {}",
                savedPayment.getId(), transactionId, savedPayment.getAmount());

        // 6. Generate digital receipt with itemized breakdown
        String receiptNumber = generateReceiptNumber();
        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .paymentId(savedPayment.getId())
                .transactionId(transactionId)
                .rideId(request.getRideId())
                .passengerId(request.getPassengerId())
                .passengerName(ride.getPassengerName())
                .driverName(ride.getDriverName())
                .vehiclePlate(ride.getVehiclePlate())
                .vehicleType(vehicleType)
                .distanceKm(distanceKm)
                .durationMinutes(fareBreakdown.durationMinutes())
                .baseFare(fareBreakdown.baseFare())
                .distanceCharge(fareBreakdown.distanceCharge())
                .timeCharge(fareBreakdown.timeCharge())
                .surgeMultiplier(fareBreakdown.surgeMultiplier())
                .totalAmount(fareBreakdown.totalFare())
                .paymentMethod(request.getPaymentMethod())
                .issuedAt(now)
                .build();

        receiptRepository.save(receipt);
        log.info("Digital receipt generated: Number={}", receiptNumber);

        return mapToPaymentResponse(savedPayment, receiptNumber);
    }

    public PaymentResponse getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        String receiptNumber = receiptRepository.findByPaymentId(payment.getId())
                .map(Receipt::getReceiptNumber)
                .orElse(null);
        return mapToPaymentResponse(payment, receiptNumber);
    }

    public PaymentResponse getPaymentByRideId(String rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride ID: " + rideId));
        String receiptNumber = receiptRepository.findByPaymentId(payment.getId())
                .map(Receipt::getReceiptNumber)
                .orElse(null);
        return mapToPaymentResponse(payment, receiptNumber);
    }

    public List<PaymentResponse> getPaymentsByPassengerId(String passengerId) {
        return paymentRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId)
                .stream()
                .map(p -> {
                    String receiptNo = receiptRepository.findByPaymentId(p.getId())
                            .map(Receipt::getReceiptNumber)
                            .orElse(null);
                    return mapToPaymentResponse(p, receiptNo);
                })
                .toList();
    }

    public ReceiptResponse getReceiptByPaymentId(String paymentId) {
        Receipt receipt = receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for payment ID: " + paymentId));
        return mapToReceiptResponse(receipt);
    }

    public ReceiptResponse getReceiptByRideId(String rideId) {
        Receipt receipt = receiptRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for ride ID: " + rideId));
        return mapToReceiptResponse(receipt);
    }

    private String generateTransactionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TXN-" + timestamp + "-" + random;
    }

    private String generateReceiptNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "REC-" + timestamp + "-" + random;
    }

    private PaymentResponse mapToPaymentResponse(Payment payment, String receiptNumber) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .rideId(payment.getRideId())
                .passengerId(payment.getPassengerId())
                .amount(payment.getAmount())
                .currency("LKR")
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .paidAt(payment.getPaidAt())
                .receiptNumber(receiptNumber)
                .build();
    }

    private ReceiptResponse mapToReceiptResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .transactionId(receipt.getTransactionId())
                .paymentId(receipt.getPaymentId())
                .rideId(receipt.getRideId())
                .passengerId(receipt.getPassengerId())
                .passengerName(receipt.getPassengerName())
                .driverName(receipt.getDriverName())
                .vehiclePlate(receipt.getVehiclePlate())
                .vehicleType(receipt.getVehicleType())
                .distanceKm(receipt.getDistanceKm())
                .durationMinutes(receipt.getDurationMinutes())
                .baseFare(receipt.getBaseFare())
                .distanceCharge(receipt.getDistanceCharge())
                .timeCharge(receipt.getTimeCharge())
                .surgeMultiplier(receipt.getSurgeMultiplier())
                .totalAmount(receipt.getTotalAmount())
                .currency("LKR")
                .paymentMethod(receipt.getPaymentMethod())
                .issuedAt(receipt.getIssuedAt())
                .build();
    }
}
