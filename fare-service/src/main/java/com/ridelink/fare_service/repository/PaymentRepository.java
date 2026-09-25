package com.ridelink.fare_service.repository;

import com.ridelink.fare_service.entity.Payment;
import com.ridelink.fare_service.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByRideId(String rideId);

    boolean existsByRideIdAndStatus(String rideId, PaymentStatus status);

    List<Payment> findByPassengerIdOrderByCreatedAtDesc(String passengerId);
}
