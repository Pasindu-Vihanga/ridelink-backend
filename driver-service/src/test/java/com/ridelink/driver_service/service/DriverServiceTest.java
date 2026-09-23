package com.ridelink.driver_service.service;

import com.ridelink.driver_service.client.AccountServiceClient;
import com.ridelink.driver_service.dto.*;
import com.ridelink.driver_service.entity.*;
import com.ridelink.driver_service.exception.DuplicateResourceException;
import com.ridelink.driver_service.exception.ResourceNotFoundException;
import com.ridelink.driver_service.repository.DriverRepository;
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
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private DriverService driverService;

    private Driver sampleDriver;
    private DriverRegistrationRequest sampleRequest;
    private AccountUserValidationDto sampleAccountUser;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = Vehicle.builder()
                .make("Toyota")
                .model("Corolla")
                .year(2022)
                .licensePlate("CAB-1234")
                .vehicleType(VehicleType.CAR)
                .capacity(4)
                .build();

        Location location = Location.builder()
                .latitude(6.9271)
                .longitude(79.8612)
                .serviceArea("Colombo")
                .lastUpdated(LocalDateTime.now())
                .build();

        sampleDriver = Driver.builder()
                .id("d1")
                .userId("u1")
                .driverName("Sunil Silva")
                .phoneNumber("+94719876543")
                .licenseNumber("DL-12345")
                .experienceYears(5)
                .rating(5.0)
                .totalTrips(0)
                .vehicle(vehicle)
                .location(location)
                .status(AvailabilityStatus.OFFLINE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = DriverRegistrationRequest.builder()
                .userId("u1")
                .driverName("Sunil Silva")
                .phoneNumber("+94719876543")
                .licenseNumber("DL-12345")
                .experienceYears(5)
                .vehicleMake("Toyota")
                .vehicleModel("Corolla")
                .vehicleYear(2022)
                .licensePlate("CAB-1234")
                .vehicleType(VehicleType.CAR)
                .vehicleCapacity(4)
                .latitude(6.9271)
                .longitude(79.8612)
                .serviceArea("Colombo")
                .build();

        sampleAccountUser = AccountUserValidationDto.builder()
                .id("u1")
                .fullName("Sunil Silva")
                .email("sunil@example.com")
                .role("DRIVER")
                .status("ACTIVE")
                .build();
    }

    @Test
    void registerDriver_Success() {
        when(driverRepository.existsByUserId("u1")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-12345")).thenReturn(false);
        when(accountServiceClient.validateDriverUser("u1")).thenReturn(sampleAccountUser);
        when(driverRepository.save(any(Driver.class))).thenReturn(sampleDriver);

        DriverResponse response = driverService.registerDriver(sampleRequest);

        assertNotNull(response);
        assertEquals("u1", response.getUserId());
        assertEquals("Sunil Silva", response.getDriverName());
        assertEquals(AvailabilityStatus.OFFLINE, response.getStatus());
        verify(accountServiceClient, times(1)).validateDriverUser("u1");
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void registerDriver_DuplicateUserId_ThrowsException() {
        when(driverRepository.existsByUserId("u1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> driverService.registerDriver(sampleRequest));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void registerDriver_DuplicateLicense_ThrowsException() {
        when(driverRepository.existsByUserId("u1")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-12345")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> driverService.registerDriver(sampleRequest));
        verify(accountServiceClient, never()).validateDriverUser(any());
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void getDriverById_Success() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(sampleDriver));

        DriverResponse response = driverService.getDriverById("d1");

        assertNotNull(response);
        assertEquals("d1", response.getId());
        assertEquals("Sunil Silva", response.getDriverName());
    }

    @Test
    void getDriverById_NotFound_ThrowsException() {
        when(driverRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> driverService.getDriverById("nonexistent"));
    }

    @Test
    void getDriverByUserId_Success() {
        when(driverRepository.findByUserId("u1")).thenReturn(Optional.of(sampleDriver));

        DriverResponse response = driverService.getDriverByUserId("u1");

        assertNotNull(response);
        assertEquals("u1", response.getUserId());
    }

    @Test
    void updateAvailability_ToOnline_Success() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenReturn(sampleDriver);

        DriverResponse response = driverService.updateAvailability("d1", AvailabilityStatus.ONLINE);

        assertNotNull(response);
        assertEquals(AvailabilityStatus.ONLINE, sampleDriver.getStatus());
        verify(driverRepository, times(1)).save(sampleDriver);
    }

    @Test
    void updateLocation_Success() {
        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .latitude(7.2906)
                .longitude(80.6337)
                .serviceArea("Kandy")
                .build();

        when(driverRepository.findById("d1")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenReturn(sampleDriver);

        DriverResponse response = driverService.updateLocation("d1", request);

        assertNotNull(response);
        assertEquals("Kandy", sampleDriver.getLocation().getServiceArea());
        verify(driverRepository, times(1)).save(sampleDriver);
    }

    @Test
    void updateTripStatus_StartTrip_SetsOnTrip() {
        sampleDriver.setStatus(AvailabilityStatus.ONLINE);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenReturn(sampleDriver);

        driverService.updateTripStatus("d1", true);

        assertEquals(AvailabilityStatus.ON_TRIP, sampleDriver.getStatus());
    }

    @Test
    void updateTripStatus_EndTrip_SetsOnlineAndIncrementsTrips() {
        sampleDriver.setStatus(AvailabilityStatus.ON_TRIP);
        when(driverRepository.findById("d1")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenReturn(sampleDriver);

        driverService.updateTripStatus("d1", false);

        assertEquals(AvailabilityStatus.ONLINE, sampleDriver.getStatus());
        assertEquals(1, sampleDriver.getTotalTrips());
    }

    @Test
    void getAvailableDrivers_ByServiceArea_Success() {
        when(driverRepository.findByStatusAndLocation_ServiceAreaIgnoreCase(
                AvailabilityStatus.ONLINE, "Colombo")).thenReturn(Collections.singletonList(sampleDriver));

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo", null);

        assertEquals(1, result.size());
        assertEquals("Sunil Silva", result.get(0).getDriverName());
    }

    @Test
    void getAvailableDrivers_ByServiceAreaAndVehicleType_Success() {
        when(driverRepository.findByStatusAndLocation_ServiceAreaIgnoreCaseAndVehicle_VehicleType(
                AvailabilityStatus.ONLINE, "Colombo", VehicleType.CAR))
                .thenReturn(Collections.singletonList(sampleDriver));

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo", VehicleType.CAR);

        assertEquals(1, result.size());
        assertEquals(VehicleType.CAR, result.get(0).getVehicle().getVehicleType());
    }

    @Test
    void getAvailableDrivers_All_Success() {
        when(driverRepository.findByStatus(AvailabilityStatus.ONLINE))
                .thenReturn(Collections.singletonList(sampleDriver));

        List<DriverResponse> result = driverService.getAvailableDrivers(null, null);

        assertEquals(1, result.size());
    }
}
