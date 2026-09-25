package com.ridelink.ride_service.service;

import com.ridelink.ride_service.client.AccountServiceClient;
import com.ridelink.ride_service.client.DriverServiceClient;
import com.ridelink.ride_service.dto.CreateRideRequest;
import com.ridelink.ride_service.dto.DriverSummaryDto;
import com.ridelink.ride_service.dto.PassengerValidationDto;
import com.ridelink.ride_service.dto.RideResponse;
import com.ridelink.ride_service.entity.LocationPoint;
import com.ridelink.ride_service.entity.Ride;
import com.ridelink.ride_service.entity.RideStatus;
import com.ridelink.ride_service.entity.VehicleType;
import com.ridelink.ride_service.exception.BadRequestException;
import com.ridelink.ride_service.exception.InvalidStateTransitionException;
import com.ridelink.ride_service.exception.NoDriverAvailableException;
import com.ridelink.ride_service.exception.ResourceNotFoundException;
import com.ridelink.ride_service.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private DriverServiceClient driverServiceClient;

    @InjectMocks
    private RideService rideService;

    private CreateRideRequest sampleRequest;
    private PassengerValidationDto samplePassenger;
    private DriverSummaryDto sampleDriver;
    private Ride sampleRide;

    @BeforeEach
    void setUp() {
        sampleRequest = CreateRideRequest.builder()
                .passengerId("p1")
                .pickupAddress("Colombo Fort")
                .pickupLatitude(6.9344)
                .pickupLongitude(79.8428)
                .serviceArea("Colombo")
                .dropoffAddress("Bambalapitiya")
                .dropoffLatitude(6.8924)
                .dropoffLongitude(79.8553)
                .vehicleType(VehicleType.CAR)
                .estimatedDistanceKm(5.0)
                .build();

        samplePassenger = PassengerValidationDto.builder()
                .id("p1")
                .fullName("Kamal Perera")
                .email("kamal@example.com")
                .phoneNumber("+94771234567")
                .role("PASSENGER")
                .status("ACTIVE")
                .build();

        sampleDriver = DriverSummaryDto.builder()
                .id("d1")
                .userId("u2")
                .driverName("Sunil Silva")
                .phoneNumber("+94719876543")
                .rating(4.8)
                .totalTrips(45)
                .vehicle(DriverSummaryDto.VehicleSummary.builder()
                        .make("Toyota")
                        .model("Corolla")
                        .licensePlate("CAB-1234")
                        .vehicleType(VehicleType.CAR)
                        .capacity(4)
                        .build())
                .status("ONLINE")
                .build();

        sampleRide = Ride.builder()
                .id("r1")
                .passengerId("p1")
                .passengerName("Kamal Perera")
                .passengerPhone("+94771234567")
                .driverId("d1")
                .driverUserId("u2")
                .driverName("Sunil Silva")
                .driverPhone("+94719876543")
                .vehiclePlate("CAB-1234")
                .vehicleModel("Corolla")
                .pickupLocation(LocationPoint.builder()
                        .address("Colombo Fort")
                        .latitude(6.9344)
                        .longitude(79.8428)
                        .serviceArea("Colombo")
                        .build())
                .dropoffLocation(LocationPoint.builder()
                        .address("Bambalapitiya")
                        .latitude(6.8924)
                        .longitude(79.8553)
                        .serviceArea("Colombo")
                        .build())
                .vehicleType(VehicleType.CAR)
                .estimatedDistanceKm(5.0)
                .fareAmount(800.0)
                .status(RideStatus.ASSIGNED)
                .requestedAt(LocalDateTime.now())
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createRide_Success() {
        when(accountServiceClient.validatePassenger("p1")).thenReturn(samplePassenger);
        when(rideRepository.findByPassengerIdAndStatusIn(eq("p1"), anyList())).thenReturn(Collections.emptyList());
        when(driverServiceClient.getAvailableDrivers("Colombo", VehicleType.CAR))
                .thenReturn(List.of(sampleDriver));
        when(rideRepository.save(any(Ride.class))).thenReturn(sampleRide);

        RideResponse response = rideService.createRide(sampleRequest);

        assertNotNull(response);
        assertEquals("p1", response.getPassengerId());
        assertEquals("d1", response.getDriverId());
        assertEquals(RideStatus.ASSIGNED, response.getStatus());

        verify(accountServiceClient, times(1)).validatePassenger("p1");
        verify(driverServiceClient, times(1)).getAvailableDrivers("Colombo", VehicleType.CAR);
        verify(driverServiceClient, times(1)).updateDriverTripStatus("d1", true);
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    void createRide_ActiveRideAlreadyExists_ThrowsBadRequest() {
        when(accountServiceClient.validatePassenger("p1")).thenReturn(samplePassenger);
        when(rideRepository.findByPassengerIdAndStatusIn(eq("p1"), anyList())).thenReturn(List.of(sampleRide));

        assertThrows(BadRequestException.class, () -> rideService.createRide(sampleRequest));

        verify(driverServiceClient, never()).getAvailableDrivers(any(), any());
        verify(rideRepository, never()).save(any());
    }

    @Test
    void createRide_NoDriversAvailable_ThrowsNoDriverAvailableException() {
        when(accountServiceClient.validatePassenger("p1")).thenReturn(samplePassenger);
        when(rideRepository.findByPassengerIdAndStatusIn(eq("p1"), anyList())).thenReturn(Collections.emptyList());
        when(driverServiceClient.getAvailableDrivers("Colombo", VehicleType.CAR)).thenReturn(Collections.emptyList());

        assertThrows(NoDriverAvailableException.class, () -> rideService.createRide(sampleRequest));

        verify(driverServiceClient, never()).updateDriverTripStatus(any(), anyBoolean());
        verify(rideRepository, never()).save(any());
    }

    @Test
    void acceptRide_FromAssigned_Success() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.acceptRide("r1");

        assertEquals(RideStatus.ACCEPTED, response.getStatus());
        assertNotNull(response.getAcceptedAt());
        verify(rideRepository, times(1)).save(sampleRide);
    }

    @Test
    void acceptRide_InvalidTransition_ThrowsException() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));

        assertThrows(InvalidStateTransitionException.class, () -> rideService.acceptRide("r1"));
    }

    @Test
    void startRide_FromAccepted_Success() {
        sampleRide.setStatus(RideStatus.ACCEPTED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.startRide("r1");

        assertEquals(RideStatus.IN_PROGRESS, response.getStatus());
        assertNotNull(response.getStartedAt());
    }

    @Test
    void startRide_InvalidTransition_ThrowsException() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));

        assertThrows(InvalidStateTransitionException.class, () -> rideService.startRide("r1"));
    }

    @Test
    void completeRide_FromInProgress_Success_ReleasesDriver() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.completeRide("r1");

        assertEquals(RideStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());
        verify(driverServiceClient, times(1)).updateDriverTripStatus("d1", false);
    }

    @Test
    void completeRide_InvalidTransition_ThrowsException() {
        sampleRide.setStatus(RideStatus.ACCEPTED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));

        assertThrows(InvalidStateTransitionException.class, () -> rideService.completeRide("r1"));
        verify(driverServiceClient, never()).updateDriverTripStatus(any(), anyBoolean());
    }

    @Test
    void cancelRide_FromAssigned_Success_ReleasesDriver() {
        sampleRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.cancelRide("r1", "Passenger changed plans");

        assertEquals(RideStatus.CANCELLED, response.getStatus());
        assertEquals("Passenger changed plans", response.getCancellationReason());
        assertNotNull(response.getCancelledAt());
        verify(driverServiceClient, times(1)).updateDriverTripStatus("d1", false);
    }

    @Test
    void cancelRide_AlreadyCompleted_ThrowsException() {
        sampleRide.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));

        assertThrows(InvalidStateTransitionException.class, () -> rideService.cancelRide("r1", "Too late"));
    }

    @Test
    void cancelRide_AlreadyCancelled_ThrowsException() {
        sampleRide.setStatus(RideStatus.CANCELLED);
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));

        assertThrows(InvalidStateTransitionException.class, () -> rideService.cancelRide("r1", "Already cancelled"));
    }

    @Test
    void getRideById_Success() {
        when(rideRepository.findById("r1")).thenReturn(Optional.of(sampleRide));

        RideResponse response = rideService.getRideById("r1");

        assertNotNull(response);
        assertEquals("r1", response.getId());
    }

    @Test
    void getRideById_NotFound_ThrowsException() {
        when(rideRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rideService.getRideById("nonexistent"));
    }

    @Test
    void getRidesByPassengerId_Success() {
        when(rideRepository.findByPassengerIdOrderByRequestedAtDesc("p1")).thenReturn(List.of(sampleRide));

        List<RideResponse> rides = rideService.getRidesByPassengerId("p1");

        assertEquals(1, rides.size());
        assertEquals("p1", rides.get(0).getPassengerId());
    }

    @Test
    void getRidesByDriverId_Success() {
        when(rideRepository.findByDriverIdOrderByRequestedAtDesc("d1")).thenReturn(List.of(sampleRide));

        List<RideResponse> rides = rideService.getRidesByDriverId("d1");

        assertEquals(1, rides.size());
        assertEquals("d1", rides.get(0).getDriverId());
    }
}
