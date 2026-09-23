package com.ridelink.account_service.dto;

import com.ridelink.account_service.entity.Role;
import jakarta.validation.constraints.NotNull;
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
public class UpdateRoleRequest {

    @NotNull(message = "Role is required (PASSENGER, DRIVER, ADMIN)")
    private Role role;
}
