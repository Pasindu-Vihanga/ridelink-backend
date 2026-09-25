package com.ridelink.fare_service.client;

import com.ridelink.fare_service.dto.RideValidationDto;
import com.ridelink.fare_service.exception.BadRequestException;
import com.ridelink.fare_service.exception.ResourceNotFoundException;
import com.ridelink.fare_service.exception.RideServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class RideServiceClient {

    private final RestClient restClient;
    private final String rideServiceUrl;

    public RideServiceClient(RestClient restClient,
                             @Value("${services.ride-service.url:http://localhost:8083}") String rideServiceUrl) {
        this.restClient = restClient;
        this.rideServiceUrl = rideServiceUrl;
    }

    public RideValidationDto getAndValidateRide(String rideId, String expectedPassengerId) {
        String url = rideServiceUrl + "/api/rides/" + rideId;
        log.info("Calling Ride Service to validate ride at: {}", url);

        try {
            RideValidationDto ride = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Ride not found in Ride Service with ID: " + rideId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RideServiceException("Ride Service is currently unavailable");
                    })
                    .body(RideValidationDto.class);

            if (ride == null) {
                throw new ResourceNotFoundException("Ride could not be retrieved from Ride Service");
            }

            if (expectedPassengerId != null && !expectedPassengerId.equals(ride.getPassengerId())) {
                throw new BadRequestException("Passenger ID does not match the ride's registered passenger");
            }

            if ("CANCELLED".equalsIgnoreCase(ride.getStatus())) {
                throw new BadRequestException("Cannot process payment for a CANCELLED ride");
            }

            return ride;
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to Ride Service: {}", e.getMessage());
            throw new RideServiceException("Unable to contact Ride Service: " + e.getMessage());
        }
    }
}
