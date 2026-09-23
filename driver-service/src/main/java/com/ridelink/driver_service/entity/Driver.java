package com.ridelink.driver_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String driverName;

    private String phoneNumber;

    @Indexed(unique = true)
    private String licenseNumber;

    private int experienceYears;

    @Builder.Default
    private double rating = 5.0;

    @Builder.Default
    private int totalTrips = 0;

    private Vehicle vehicle;

    private Location location;

    @Builder.Default
    private AvailabilityStatus status = AvailabilityStatus.OFFLINE;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
