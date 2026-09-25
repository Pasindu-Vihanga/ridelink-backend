package com.ridelink.ride_service.client;

import com.ridelink.ride_service.dto.DriverSummaryDto;
import com.ridelink.ride_service.entity.VehicleType;
import com.ridelink.ride_service.exception.DriverServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DriverServiceClient {

    private final RestClient restClient;
    private final String driverServiceUrl;

    public DriverServiceClient(RestClient restClient,
                               @Value("${services.driver-service.url:http://localhost:8082}") String driverServiceUrl) {
        this.restClient = restClient;
        this.driverServiceUrl = driverServiceUrl;
    }

    public List<DriverSummaryDto> getAvailableDrivers(String serviceArea, VehicleType vehicleType) {
        String url = driverServiceUrl + "/api/drivers/available?serviceArea=" + serviceArea;
        if (vehicleType != null) {
            url += "&vehicleType=" + vehicleType.name();
        }
        log.info("Calling Driver Service to find available drivers at: {}", url);

        try {
            List<DriverSummaryDto> drivers = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Driver Service returned 5xx server error when fetching available drivers");
                        throw new DriverServiceException("Driver Service is currently unavailable");
                    })
                    .body(new ParameterizedTypeReference<List<DriverSummaryDto>>() {});

            return drivers != null ? drivers : Collections.emptyList();
        } catch (DriverServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to Driver Service: {}", e.getMessage());
            throw new DriverServiceException("Unable to contact Driver Service: " + e.getMessage());
        }
    }

    public void updateDriverTripStatus(String driverId, boolean onTrip) {
        String url = driverServiceUrl + "/api/drivers/" + driverId + "/trip-status";
        log.info("Updating trip status for driver {} to onTrip={} at {}", driverId, onTrip, url);

        try {
            restClient.patch()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("onTrip", onTrip))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Failed to update trip status for driver {} in Driver Service", driverId);
                        throw new DriverServiceException("Failed to update driver trip status in Driver Service");
                    })
                    .toBodilessEntity();
            log.info("Successfully updated trip status for driver {}", driverId);
        } catch (Exception e) {
            log.warn("Could not update trip status in Driver Service for driver {}: {}", driverId, e.getMessage());
        }
    }
}
