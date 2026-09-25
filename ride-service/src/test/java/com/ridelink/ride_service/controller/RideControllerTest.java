package com.ridelink.ride_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride_service.dto.CancelRideRequest;
import com.ridelink.ride_service.dto.CreateRideRequest;
import com.ridelink.ride_service.dto.RideResponse;
import com.ridelink.ride_service.entity.LocationPoint;
import com.ridelink.ride_service.entity.RideStatus;
import com.ridelink.ride_service.entity.VehicleType;
import com.ridelink.ride_service.exception.GlobalExceptionHandler;
import com.ridelink.ride_service.exception.InvalidStateTransitionException;
import com.ridelink.ride_service.exception.NoDriverAvailableException;
import com.ridelink.ride_service.exception.ResourceNotFoundException;
import com.ridelink.ride_service.service.RideService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RideService rideService;

    @InjectMocks
    private RideController driverController;

    private RideResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(driverController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = RideResponse.builder()
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
    void createRide_ValidRequest_Returns201Created() throws Exception {
        CreateRideRequest request = CreateRideRequest.builder()
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

        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("r1"))
                .andExpect(jsonPath("$.passengerId").value("p1"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void createRide_MissingFields_Returns400BadRequest() throws Exception {
        CreateRideRequest request = CreateRideRequest.builder()
                .passengerId("")
                .pickupAddress("")
                .build();

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createRide_NoDriverAvailable_Returns404() throws Exception {
        CreateRideRequest request = CreateRideRequest.builder()
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

        when(rideService.createRide(any(CreateRideRequest.class)))
                .thenThrow(new NoDriverAvailableException("No available drivers found"));

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No Driver Available"));
    }

    @Test
    void getRideById_Success() throws Exception {
        when(rideService.getRideById("r1")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/rides/r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("r1"))
                .andExpect(jsonPath("$.driverName").value("Sunil Silva"));
    }

    @Test
    void getRideById_NotFound_Returns404() throws Exception {
        when(rideService.getRideById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Ride not found"));

        mockMvc.perform(get("/api/rides/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void acceptRide_Success_Returns200() throws Exception {
        sampleResponse.setStatus(RideStatus.ACCEPTED);
        when(rideService.acceptRide("r1")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/rides/r1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void startRide_Success_Returns200() throws Exception {
        sampleResponse.setStatus(RideStatus.IN_PROGRESS);
        when(rideService.startRide("r1")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/rides/r1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void completeRide_Success_Returns200() throws Exception {
        sampleResponse.setStatus(RideStatus.COMPLETED);
        when(rideService.completeRide("r1")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/rides/r1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void cancelRide_Success_Returns200() throws Exception {
        sampleResponse.setStatus(RideStatus.CANCELLED);
        sampleResponse.setCancellationReason("Changed destination");
        CancelRideRequest cancelRequest = new CancelRideRequest("Changed destination");

        when(rideService.cancelRide(eq("r1"), eq("Changed destination"))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/rides/r1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Changed destination"));
    }

    @Test
    void invalidStateTransition_Returns400BadRequest() throws Exception {
        when(rideService.startRide("r1"))
                .thenThrow(new InvalidStateTransitionException("Cannot start ride from status ASSIGNED"));

        mockMvc.perform(patch("/api/rides/r1/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request - Invalid State Transition"));
    }

    @Test
    void getRidesByPassengerId_ReturnsList() throws Exception {
        when(rideService.getRidesByPassengerId("p1")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/rides/passenger/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].passengerId").value("p1"));
    }
}
