package com.ridelink.driver_service.dto;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateLocationRequest {

    private double latitude;

    private double longitude;

    @NotBlank(message = "Service area is required (e.g. Colombo, Kandy)")
    private String serviceArea;
}
