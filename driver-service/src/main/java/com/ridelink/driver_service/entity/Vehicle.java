package com.ridelink.driver_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    private String make;

    private String model;

    private int year;

    private String licensePlate;

    private VehicleType vehicleType;

    private int capacity;
}
