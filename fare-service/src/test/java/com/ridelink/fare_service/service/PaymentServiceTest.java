package com.ridelink.fare_service.service;

import com.ridelink.fare_service.client.RideServiceClient;
import com.ridelink.fare_service.dto.PaymentResponse;
import com.ridelink.fare_service.dto.ProcessPaymentRequest;
import com.ridelink.fare_service.dto.ReceiptResponse;
import com.ridelink.fare_service.dto.RideValidationDto;
import com.ridelink.fare_service.entity.*;
import com.ridelink.fare_service.exception.DuplicatePaymentException;
import com.ridelink.fare_service.exception.PaymentFailedException;
import com.ridelink.fare_service.exception.ResourceNotFoundException;
import com.ridelink.fare_service.repository.PaymentRepository;
import com.ridelink.fare_service.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private RideServiceClient rideServiceClient;

    @Spy
    private FareCalculationService fareCalculationService;

    @InjectMocks
    private PaymentService paymentService;

    private ProcessPaymentRequest sampleRequest;
    private RideValidationDto sampleRide;
    private Payment samplePayment;
    private Receipt sampleReceipt;

    @BeforeEach
    void setUp() {
        sampleRequest = ProcessPaymentRequest.builder()
                .rideId("r1")
                .passengerId("p1")
                .paymentMethod(PaymentMethod.CARD)
                .actualDistanceKm(5.0)
                .actualDurationMinutes(15.0)
                .simulateFailure(false)
                .build();

        sampleRide = RideValidationDto.builder()
                .id("r1")
                .passengerId("p1")
                .passengerName("Kamal Perera")
                .driverId("d1")
                .driverName("Sunil Silva")
                .vehiclePlate("CAB-1234")
                .vehicleType(VehicleType.CAR)
                .estimatedDistanceKm(5.0)
                .status("COMPLETED")
                .fareAmount(800.0)
                .build();

        samplePayment = Payment.builder()
                .id("pay1")
                .transactionId("TXN-20260926-123456")
                .rideId("r1")
                .passengerId("p1")
                .amount(950.0)
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        sampleReceipt = Receipt.builder()
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
                .paymentMethod(PaymentMethod.CARD)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processPayment_Success() {
        when(rideServiceClient.getAndValidateRide("r1", "p1")).thenReturn(sampleRide);
        when(paymentRepository.existsByRideIdAndStatus("r1", PaymentStatus.COMPLETED)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(samplePayment);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(sampleReceipt);

        PaymentResponse response = paymentService.processPayment(sampleRequest);

        assertNotNull(response);
        assertEquals("pay1", response.getId());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
        assertNotNull(response.getReceiptNumber());

        verify(rideServiceClient, times(1)).getAndValidateRide("r1", "p1");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void processPayment_DuplicatePayment_ThrowsDuplicatePaymentException() {
        when(rideServiceClient.getAndValidateRide("r1", "p1")).thenReturn(sampleRide);
        when(paymentRepository.existsByRideIdAndStatus("r1", PaymentStatus.COMPLETED)).thenReturn(true);

        assertThrows(DuplicatePaymentException.class, () -> paymentService.processPayment(sampleRequest));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void processPayment_SimulateFailure_ThrowsPaymentFailedException() {
        sampleRequest.setSimulateFailure(true);
        when(rideServiceClient.getAndValidateRide("r1", "p1")).thenReturn(sampleRide);
        when(paymentRepository.existsByRideIdAndStatus("r1", PaymentStatus.COMPLETED)).thenReturn(false);

        assertThrows(PaymentFailedException.class, () -> paymentService.processPayment(sampleRequest));

        verify(paymentRepository, times(1)).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void getPaymentById_Success() {
        when(paymentRepository.findById("pay1")).thenReturn(Optional.of(samplePayment));
        when(receiptRepository.findByPaymentId("pay1")).thenReturn(Optional.of(sampleReceipt));

        PaymentResponse response = paymentService.getPaymentById("pay1");

        assertNotNull(response);
        assertEquals("pay1", response.getId());
        assertEquals("REC-20260926-654321", response.getReceiptNumber());
    }

    @Test
    void getPaymentById_NotFound_ThrowsException() {
        when(paymentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById("nonexistent"));
    }

    @Test
    void getReceiptByPaymentId_Success() {
        when(receiptRepository.findByPaymentId("pay1")).thenReturn(Optional.of(sampleReceipt));

        ReceiptResponse response = paymentService.getReceiptByPaymentId("pay1");

        assertNotNull(response);
        assertEquals("REC-20260926-654321", response.getReceiptNumber());
        assertEquals("Kamal Perera", response.getPassengerName());
        assertEquals(950.0, response.getTotalAmount());
    }
}
