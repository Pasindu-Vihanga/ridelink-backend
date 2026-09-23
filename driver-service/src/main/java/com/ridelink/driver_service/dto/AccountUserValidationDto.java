package com.ridelink.driver_service.dto;

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
public class AccountUserValidationDto {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String status;
}
