package com.ridelink.account_service.dto;

import com.ridelink.account_service.entity.AccountStatus;
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
public class UpdateStatusRequest {

    @NotNull(message = "Status is required (ACTIVE, SUSPENDED, DEACTIVATED)")
    private AccountStatus status;
}
