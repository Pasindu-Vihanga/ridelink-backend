package com.ridelink.driver_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver_service.dto.*;
import com.ridelink.driver_service.entity.*;
import com.ridelink.driver_service.exception.DuplicateResourceException;
import com.ridelink.driver_service.exception.GlobalExceptionHandler;
import com.ridelink.driver_service.exception.ResourceNotFoundException;
import com.ridelink.driver_service.service.DriverService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private DriverController driverController;

    private DriverResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(driverController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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

        sampleResponse = DriverResponse.builder()
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
    }

    @Test
    void registerDriver_ValidRequest_ReturnsCreated() throws Exception {
        DriverRegistrationRequest request = DriverRegistrationRequest.builder()
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

        when(driverService.registerDriver(any(DriverRegistrationRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("d1"))
                .andExpect(jsonPath("$.driverName").value("Sunil Silva"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    void registerDriver_MissingFields_ReturnsBadRequest() throws Exception {
        DriverRegistrationRequest request = DriverRegistrationRequest.builder()
                .userId("")
                .driverName("")
                .build();

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registerDriver_DuplicateUserId_ReturnsConflict() throws Exception {
        DriverRegistrationRequest request = DriverRegistrationRequest.builder()
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
                .serviceArea("Colombo")
                .build();

        when(driverService.registerDriver(any(DriverRegistrationRequest.class)))
                .thenThrow(new DuplicateResourceException("Driver profile already exists"));

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getDriverById_Success() throws Exception {
        when(driverService.getDriverById("d1")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/drivers/d1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("d1"))
                .andExpect(jsonPath("$.driverName").value("Sunil Silva"));
    }

    @Test
    void getDriverById_NotFound_Returns404() throws Exception {
        when(driverService.getDriverById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Driver not found"));

        mockMvc.perform(get("/api/drivers/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getDriverByUserId_Success() throws Exception {
        when(driverService.getDriverByUserId("u1")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/drivers/user/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    void updateAvailability_ToOnline_ReturnsOk() throws Exception {
        sampleResponse.setStatus(AvailabilityStatus.ONLINE);

        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .status(AvailabilityStatus.ONLINE)
                .build();

        when(driverService.updateAvailability("d1", AvailabilityStatus.ONLINE)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/drivers/d1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void getAvailableDrivers_FilteredByServiceArea_ReturnsList() throws Exception {
        when(driverService.getAvailableDrivers("Colombo", null))
                .thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/drivers/available")
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
