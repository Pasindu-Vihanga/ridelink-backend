package com.ridelink.driver_service.service;

import com.ridelink.driver_service.client.AccountServiceClient;
import com.ridelink.driver_service.dto.*;
import com.ridelink.driver_service.entity.*;
import com.ridelink.driver_service.exception.DuplicateResourceException;
import com.ridelink.driver_service.exception.ResourceNotFoundException;
import com.ridelink.driver_service.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final AccountServiceClient accountServiceClient;

    public DriverResponse registerDriver(DriverRegistrationRequest request) {
        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException("Driver profile already exists for user ID: " + request.getUserId());
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("Driver license number is already registered: " + request.getLicenseNumber());
        }

        // Interservice validation: Check if user exists and is ACTIVE with DRIVER role
        AccountUserValidationDto accountUser = accountServiceClient.validateDriverUser(request.getUserId());
        log.info("Driver user verified with Account Service: {} ({})", accountUser.getFullName(), accountUser.getEmail());

        Vehicle vehicle = Vehicle.builder()
                .make(request.getVehicleMake().trim())
                .model(request.getVehicleModel().trim())
                .year(request.getVehicleYear())
                .licensePlate(request.getLicensePlate().trim().toUpperCase())
                .vehicleType(request.getVehicleType())
                .capacity(request.getVehicleCapacity())
                .build();

        Location location = Location.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .serviceArea(request.getServiceArea().trim())
                .lastUpdated(LocalDateTime.now())
                .build();

        Driver driver = Driver.builder()
                .userId(request.getUserId())
                .driverName(request.getDriverName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .licenseNumber(request.getLicenseNumber().trim().toUpperCase())
                .experienceYears(request.getExperienceYears())
                .rating(5.0)
                .totalTrips(0)
                .vehicle(vehicle)
                .location(location)
                .status(AvailabilityStatus.OFFLINE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Driver saved = driverRepository.save(driver);
        return DriverResponse.fromDriver(saved);
    }

    public DriverResponse getDriverById(String id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return DriverResponse.fromDriver(driver);
    }

    public DriverResponse getDriverByUserId(String userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with user ID: " + userId));
        return DriverResponse.fromDriver(driver);
    }

    public DriverResponse updateVehicle(String driverId, UpdateVehicleRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        Vehicle vehicle = Vehicle.builder()
                .make(request.getMake().trim())
                .model(request.getModel().trim())
                .year(request.getYear())
                .licensePlate(request.getLicensePlate().trim().toUpperCase())
                .vehicleType(request.getVehicleType())
                .capacity(request.getCapacity())
                .build();

        driver.setVehicle(vehicle);
        driver.setUpdatedAt(LocalDateTime.now());

        Driver updated = driverRepository.save(driver);
        return DriverResponse.fromDriver(updated);
    }

    public DriverResponse updateAvailability(String driverId, AvailabilityStatus status) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        driver.setStatus(status);
        driver.setUpdatedAt(LocalDateTime.now());

        Driver updated = driverRepository.save(driver);
        return DriverResponse.fromDriver(updated);
    }

    public DriverResponse updateLocation(String driverId, UpdateLocationRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        Location location = Location.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .serviceArea(request.getServiceArea().trim())
                .lastUpdated(LocalDateTime.now())
                .build();

        driver.setLocation(location);
        driver.setUpdatedAt(LocalDateTime.now());

        Driver updated = driverRepository.save(driver);
        return DriverResponse.fromDriver(updated);
    }

    public DriverResponse updateTripStatus(String driverId, boolean onTrip) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        if (onTrip) {
            driver.setStatus(AvailabilityStatus.ON_TRIP);
        } else {
            driver.setStatus(AvailabilityStatus.ONLINE);
            driver.setTotalTrips(driver.getTotalTrips() + 1);
        }
        driver.setUpdatedAt(LocalDateTime.now());

        Driver updated = driverRepository.save(driver);
        return DriverResponse.fromDriver(updated);
    }

    public List<DriverResponse> getAvailableDrivers(String serviceArea, VehicleType vehicleType) {
        List<Driver> drivers;

        if (serviceArea != null && !serviceArea.isBlank() && vehicleType != null) {
            drivers = driverRepository.findByStatusAndLocation_ServiceAreaIgnoreCaseAndVehicle_VehicleType(
                    AvailabilityStatus.ONLINE, serviceArea.trim(), vehicleType);
        } else if (serviceArea != null && !serviceArea.isBlank()) {
            drivers = driverRepository.findByStatusAndLocation_ServiceAreaIgnoreCase(
                    AvailabilityStatus.ONLINE, serviceArea.trim());
        } else {
            drivers = driverRepository.findByStatus(AvailabilityStatus.ONLINE);
        }

        return drivers.stream()
                .map(DriverResponse::fromDriver)
                .collect(Collectors.toList());
    }
}
