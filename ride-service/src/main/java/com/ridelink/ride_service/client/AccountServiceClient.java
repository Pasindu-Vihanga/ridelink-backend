package com.ridelink.ride_service.client;

import com.ridelink.ride_service.dto.PassengerValidationDto;
import com.ridelink.ride_service.exception.AccountServiceException;
import com.ridelink.ride_service.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AccountServiceClient {

    private final RestClient restClient;
    private final String accountServiceUrl;

    public AccountServiceClient(RestClient restClient,
                                @Value("${services.account-service.url:http://localhost:8081}") String accountServiceUrl) {
        this.restClient = restClient;
        this.accountServiceUrl = accountServiceUrl;
    }

    public PassengerValidationDto validatePassenger(String userId) {
        String url = accountServiceUrl + "/api/users/" + userId + "/validate";
        log.info("Calling Account Service to validate passenger at: {}", url);

        try {
            PassengerValidationDto passenger = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("Account Service returned client error for passenger ID: {}", userId);
                        throw new BadRequestException("Passenger validation failed: Account not found or invalid in Account Service");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Account Service returned 5xx server error for passenger ID: {}", userId);
                        throw new AccountServiceException("Account Service is currently unavailable");
                    })
                    .body(PassengerValidationDto.class);

            if (passenger == null) {
                throw new BadRequestException("Passenger could not be verified in Account Service");
            }

            if (!"ACTIVE".equalsIgnoreCase(passenger.getStatus())) {
                throw new BadRequestException("Passenger account is not ACTIVE. Current status: " + passenger.getStatus());
            }

            if (!"PASSENGER".equalsIgnoreCase(passenger.getRole()) && !"ADMIN".equalsIgnoreCase(passenger.getRole())) {
                throw new BadRequestException("User role must be PASSENGER to request a ride. Current role: " + passenger.getRole());
            }

            return passenger;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to Account Service: {}", e.getMessage());
            throw new AccountServiceException("Unable to contact Account Service: " + e.getMessage());
        }
    }
}
