package com.ridelink.driver_service.repository;

import com.ridelink.driver_service.entity.AvailabilityStatus;
import com.ridelink.driver_service.entity.Driver;
import com.ridelink.driver_service.entity.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {

    Optional<Driver> findByUserId(String userId);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByUserId(String userId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByStatus(AvailabilityStatus status);

    List<Driver> findByStatusAndLocation_ServiceAreaIgnoreCase(AvailabilityStatus status, String serviceArea);

    List<Driver> findByStatusAndLocation_ServiceAreaIgnoreCaseAndVehicle_VehicleType(
            AvailabilityStatus status, String serviceArea, VehicleType vehicleType);
}
