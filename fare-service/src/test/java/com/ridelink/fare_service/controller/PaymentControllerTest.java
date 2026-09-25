package com.ridelink.fare_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fare_service.dto.PaymentResponse;
import com.ridelink.fare_service.dto.ProcessPaymentRequest;
import com.ridelink.fare_service.dto.ReceiptResponse;
import com.ridelink.fare_service.entity.PaymentMethod;
import com.ridelink.fare_service.entity.PaymentStatus;
import com.ridelink.fare_service.entity.VehicleType;
import com.ridelink.fare_service.exception.DuplicatePaymentException;
import com.ridelink.fare_service.exception.GlobalExceptionHandler;
import com.ridelink.fare_service.exception.PaymentFailedException;
import com.ridelink.fare_service.exception.ResourceNotFoundException;
import com.ridelink.fare_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentResponse samplePaymentResponse;
    private ReceiptResponse sampleReceiptResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        samplePaymentResponse = PaymentResponse.builder()
                .id("pay1")
                .transactionId("TXN-20260926-123456")
                .rideId("r1")
                .passengerId("p1")
                .amount(950.0)
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .receiptNumber("REC-20260926-654321")
                .build();

        sampleReceiptResponse = ReceiptResponse.builder()
                .id("rec1")
                .receiptNumber("REC-20260926-654321")
                .paymentId("pay1")
                .transactionId("TXN-20260926-123456")
                .rideId("r1")
                .passengerId("p1")
                .passengerName("Kamal Perera")
                .driverName("Sunil Silva")
                .vehiclePlate("CAB-1234")
                .vehicleType(VehicleType.CAR)
                .distanceKm(5.0)
                .durationMinutes(15.0)
                .baseFare(200.0)
                .distanceCharge(600.0)
                .timeCharge(150.0)
                .surgeMultiplier(1.0)
                .totalAmount(950.0)
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processPayment_ValidRequest_Returns201Created() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("r1")
                .passengerId("p1")
                .paymentMethod(PaymentMethod.CARD)
                .actualDistanceKm(5.0)
                .actualDurationMinutes(15.0)
                .build();

        when(paymentService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(samplePaymentResponse);

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.receiptNumber").value("REC-20260926-654321"));
    }

    @Test
    void processPayment_MissingFields_Returns400BadRequest() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("")
                .passengerId("")
                .build();

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void processPayment_Duplicate_Returns409Conflict() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("r1")
                .passengerId("p1")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentService.processPayment(any(ProcessPaymentRequest.class)))
                .thenThrow(new DuplicatePaymentException("Payment already recorded"));

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Payment"));
    }

    @Test
    void processPayment_PaymentFailed_Returns402PaymentRequired() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("r1")
                .passengerId("p1")
                .paymentMethod(PaymentMethod.CARD)
                .simulateFailure(true)
                .build();

        when(paymentService.processPayment(any(ProcessPaymentRequest.class)))
                .thenThrow(new PaymentFailedException("Transaction declined"));

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("Payment Failed"));
    }

    @Test
    void getPaymentById_Success() throws Exception {
        when(paymentService.getPaymentById("pay1")).thenReturn(samplePaymentResponse);

        mockMvc.perform(get("/api/payments/pay1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay1"))
                .andExpect(jsonPath("$.amount").value(950.0));
    }

    @Test
    void getPaymentById_NotFound_Returns404() throws Exception {
        when(paymentService.getPaymentById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/payments/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getReceiptByPaymentId_Success() throws Exception {
        when(paymentService.getReceiptByPaymentId("pay1")).thenReturn(sampleReceiptResponse);

        mockMvc.perform(get("/api/payments/pay1/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("REC-20260926-654321"))
                .andExpect(jsonPath("$.passengerName").value("Kamal Perera"))
                .andExpect(jsonPath("$.totalAmount").value(950.0));
    }
}
