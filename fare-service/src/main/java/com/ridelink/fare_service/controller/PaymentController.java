package com.ridelink.fare_service.controller;

import com.ridelink.fare_service.dto.PaymentResponse;
import com.ridelink.fare_service.dto.ProcessPaymentRequest;
import com.ridelink.fare_service.dto.ReceiptResponse;
import com.ridelink.fare_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for simulated payment processing, transaction records, and digital receipts")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Process simulated payment for a ride", description = "Validates ride with Ride Service, computes final fare, records payment, and creates digital receipt.")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment transaction by payment ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get payment transaction for a specific ride ID")
    public ResponseEntity<PaymentResponse> getPaymentByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }

    @GetMapping("/passenger/{passengerId}")
    @Operation(summary = "Get all payments for a passenger")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByPassengerId(@PathVariable String passengerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByPassengerId(passengerId));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Get itemized digital receipt by payment ID")
    public ResponseEntity<ReceiptResponse> getReceiptByPaymentId(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getReceiptByPaymentId(id));
    }

    @GetMapping("/ride/{rideId}/receipt")
    @Operation(summary = "Get itemized digital receipt by ride ID")
    public ResponseEntity<ReceiptResponse> getReceiptByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getReceiptByRideId(rideId));
    }
}
