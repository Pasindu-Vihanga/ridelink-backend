package com.ridelink.driver_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    private double latitude;

    private double longitude;

    private String serviceArea;

    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
