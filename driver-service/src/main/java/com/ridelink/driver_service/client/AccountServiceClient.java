package com.ridelink.driver_service.client;

import com.ridelink.driver_service.dto.AccountUserValidationDto;
import com.ridelink.driver_service.exception.AccountServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.account-service.url:http://localhost:8081}") String accountServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(accountServiceUrl).build();
    }

    public AccountUserValidationDto validateDriverUser(String userId) {
        try {
            AccountUserValidationDto user = restClient.get()
                    .uri("/api/users/{id}/validate", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AccountServiceException("User validation failed in Account Service: status " + response.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AccountServiceException("Account Service is temporarily unavailable");
                    })
                    .body(AccountUserValidationDto.class);

            if (user == null) {
                throw new AccountServiceException("User not found in Account Service with ID: " + userId);
            }

            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new AccountServiceException("User account is not ACTIVE in Account Service (Status: " + user.getStatus() + ")");
            }

            if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
                throw new AccountServiceException("User does not have DRIVER role in Account Service (Role: " + user.getRole() + ")");
            }

            return user;
        } catch (AccountServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to connect to Account Service: {}", ex.getMessage());
            throw new AccountServiceException("Unable to communicate with Account Service: " + ex.getMessage());
        }
    }
}
