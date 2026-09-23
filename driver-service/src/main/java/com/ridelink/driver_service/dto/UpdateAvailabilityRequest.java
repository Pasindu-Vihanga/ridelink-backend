package com.ridelink.driver_service.dto;

import com.ridelink.driver_service.entity.AvailabilityStatus;
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
public class UpdateAvailabilityRequest {

    @NotNull(message = "Availability status is required (ONLINE, OFFLINE, ON_TRIP)")
    private AvailabilityStatus status;
}
