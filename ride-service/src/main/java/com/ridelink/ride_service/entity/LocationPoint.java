package com.ridelink.ride_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationPoint {
    private String address;
    private Double latitude;
    private Double longitude;
    private String serviceArea;
}
