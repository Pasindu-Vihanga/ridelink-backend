package com.ridelink.ride_service.service;

import com.ridelink.ride_service.client.AccountServiceClient;
import com.ridelink.ride_service.client.DriverServiceClient;
import com.ridelink.ride_service.dto.*;
import com.ridelink.ride_service.entity.*;
import com.ridelink.ride_service.exception.BadRequestException;
import com.ridelink.ride_service.exception.InvalidStateTransitionException;
import com.ridelink.ride_service.exception.NoDriverAvailableException;
import com.ridelink.ride_service.exception.ResourceNotFoundException;
import com.ridelink.ride_service.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final AccountServiceClient accountServiceClient;
    private final DriverServiceClient driverServiceClient;

    /**
     * Create ride request and auto-assign an eligible driver using documented selection strategy.
     */
    public RideResponse createRide(CreateRideRequest request) {
        log.info("Processing ride request for passenger ID: {}", request.getPassengerId());

        // 1. Interservice Validation: Account Service (Passenger exists, ACTIVE, PASSENGER role)
        PassengerValidationDto passenger = accountServiceClient.validatePassenger(request.getPassengerId());
        log.info("Passenger validated successfully: {}", passenger.getFullName());

        // 2. Business Constraint: Check for existing active ride for passenger
        List<RideStatus> activeStatuses = List.of(
                RideStatus.REQUESTED,
                RideStatus.ASSIGNED,
                RideStatus.ACCEPTED,
                RideStatus.IN_PROGRESS
        );
        List<Ride> activeRides = rideRepository.findByPassengerIdAndStatusIn(request.getPassengerId(), activeStatuses);
        if (!activeRides.isEmpty()) {
            throw new BadRequestException("Passenger already has an ongoing active ride (Ride ID: " + activeRides.get(0).getId() + ")");
        }

        // 3. Interservice Query: Find available drivers matching serviceArea and vehicleType
        List<DriverSummaryDto> availableDrivers = driverServiceClient.getAvailableDrivers(
                request.getServiceArea(),
                request.getVehicleType()
        );

        if (availableDrivers == null || availableDrivers.isEmpty()) {
            throw new NoDriverAvailableException(
                    "No available " + request.getVehicleType() + " drivers found in service area: " + request.getServiceArea()
            );
        }

        // 4. Selection Strategy: Prioritize highest rating, then highest total trips
        DriverSummaryDto selectedDriver = availableDrivers.stream()
                .max(Comparator.comparingDouble((DriverSummaryDto d) -> d.getRating() != null ? d.getRating() : 0.0)
                        .thenComparingInt(d -> d.getTotalTrips() != null ? d.getTotalTrips() : 0))
                .orElse(availableDrivers.get(0));

        log.info("Assigned driver {} ({}) to ride request", selectedDriver.getDriverName(), selectedDriver.getId());

        // 5. Calculate base fare estimate
        double estimatedFare = calculateFareEstimate(request.getVehicleType(), request.getEstimatedDistanceKm());

        // 6. Notify Driver Service to mark driver as ON_TRIP
        driverServiceClient.updateDriverTripStatus(selectedDriver.getId(), true);

        // 7. Persist Ride entity
        LocalDateTime now = LocalDateTime.now();
        Ride ride = Ride.builder()
                .passengerId(passenger.getId())
                .passengerName(passenger.getFullName())
                .passengerPhone(passenger.getPhoneNumber())
                .driverId(selectedDriver.getId())
                .driverUserId(selectedDriver.getUserId())
                .driverName(selectedDriver.getDriverName())
                .driverPhone(selectedDriver.getPhoneNumber())
                .vehiclePlate(selectedDriver.getVehicle() != null ? selectedDriver.getVehicle().getLicensePlate() : "N/A")
                .vehicleModel(selectedDriver.getVehicle() != null ? selectedDriver.getVehicle().getModel() : "N/A")
                .pickupLocation(LocationPoint.builder()
                        .address(request.getPickupAddress())
                        .latitude(request.getPickupLatitude())
                        .longitude(request.getPickupLongitude())
                        .serviceArea(request.getServiceArea())
                        .build())
                .dropoffLocation(LocationPoint.builder()
                        .address(request.getDropoffAddress())
                        .latitude(request.getDropoffLatitude())
                        .longitude(request.getDropoffLongitude())
                        .serviceArea(request.getServiceArea())
                        .build())
                .vehicleType(request.getVehicleType())
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .fareAmount(estimatedFare)
                .status(RideStatus.ASSIGNED)
                .requestedAt(now)
                .assignedAt(now)
                .build();

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride created and assigned successfully with ID: {}", savedRide.getId());
        return mapToResponse(savedRide);
    }

    /**
     * Driver accepts assigned ride.
     * Transition: ASSIGNED -> ACCEPTED
     */
    public RideResponse acceptRide(String rideId) {
        Ride ride = getRideEntity(rideId);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new InvalidStateTransitionException(
                    "Cannot accept ride from status " + ride.getStatus() + ". Must be ASSIGNED."
            );
        }

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        return mapToResponse(rideRepository.save(ride));
    }

    /**
     * Driver starts trip.
     * Transition: ACCEPTED -> IN_PROGRESS
     */
    public RideResponse startRide(String rideId) {
        Ride ride = getRideEntity(rideId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidStateTransitionException(
                    "Cannot start ride from status " + ride.getStatus() + ". Must be ACCEPTED."
            );
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());
        return mapToResponse(rideRepository.save(ride));
    }

    /**
     * Driver completes trip.
     * Transition: IN_PROGRESS -> COMPLETED
     * Releases driver back to ONLINE in Driver Service.
     */
    public RideResponse completeRide(String rideId) {
        Ride ride = getRideEntity(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Cannot complete ride from status " + ride.getStatus() + ". Must be IN_PROGRESS."
            );
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());

        // Interservice: Mark driver back ONLINE and increment trip count
        if (ride.getDriverId() != null) {
            driverServiceClient.updateDriverTripStatus(ride.getDriverId(), false);
        }

        return mapToResponse(rideRepository.save(ride));
    }

    /**
     * Cancel ride by passenger or driver.
     * Allowed from REQUESTED, ASSIGNED, ACCEPTED.
     * Not allowed once COMPLETED or already CANCELLED.
     */
    public RideResponse cancelRide(String rideId, String reason) {
        Ride ride = getRideEntity(rideId);

        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Cannot cancel a ride that has already been COMPLETED.");
        }
        if (ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Ride is already CANCELLED.");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Cancelled by user");
        ride.setCancelledAt(LocalDateTime.now());

        // Interservice: Release driver if one was assigned
        if (ride.getDriverId() != null) {
            driverServiceClient.updateDriverTripStatus(ride.getDriverId(), false);
        }

        return mapToResponse(rideRepository.save(ride));
    }

    public RideResponse getRideById(String rideId) {
        return mapToResponse(getRideEntity(rideId));
    }

    public List<RideResponse> getRidesByPassengerId(String passengerId) {
        return rideRepository.findByPassengerIdOrderByRequestedAtDesc(passengerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<RideResponse> getRidesByDriverId(String driverId) {
        return rideRepository.findByDriverIdOrderByRequestedAtDesc(driverId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<RideResponse> getAllRides(RideStatus status) {
        if (status != null) {
            return rideRepository.findByStatus(status)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return rideRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Ride getRideEntity(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));
    }

    private double calculateFareEstimate(VehicleType vehicleType, double distanceKm) {
        double baseRate;
        double perKmRate;

        switch (vehicleType) {
            case CAR -> {
                baseRate = 200.0;
                perKmRate = 120.0;
            }
            case VAN -> {
                baseRate = 350.0;
                perKmRate = 160.0;
            }
            case BIKE -> {
                baseRate = 80.0;
                perKmRate = 50.0;
            }
            case TUK -> {
                baseRate = 100.0;
                perKmRate = 80.0;
            }
            default -> {
                baseRate = 150.0;
                perKmRate = 100.0;
            }
        }

        return Math.round((baseRate + (perKmRate * distanceKm)) * 100.0) / 100.0;
    }

    private RideResponse mapToResponse(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .passengerId(ride.getPassengerId())
                .passengerName(ride.getPassengerName())
                .passengerPhone(ride.getPassengerPhone())
                .driverId(ride.getDriverId())
                .driverUserId(ride.getDriverUserId())
                .driverName(ride.getDriverName())
                .driverPhone(ride.getDriverPhone())
                .vehiclePlate(ride.getVehiclePlate())
                .vehicleModel(ride.getVehicleModel())
                .pickupLocation(ride.getPickupLocation())
                .dropoffLocation(ride.getDropoffLocation())
                .vehicleType(ride.getVehicleType())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .status(ride.getStatus())
                .fareAmount(ride.getFareAmount())
                .cancellationReason(ride.getCancellationReason())
                .requestedAt(ride.getRequestedAt())
                .assignedAt(ride.getAssignedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .build();
    }
}
