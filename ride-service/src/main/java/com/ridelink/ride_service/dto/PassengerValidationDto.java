package com.ridelink.ride_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerValidationDto {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String status;
}
