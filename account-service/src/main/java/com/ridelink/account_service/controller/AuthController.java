package com.ridelink.account_service.controller;

import com.ridelink.account_service.dto.LoginRequest;
import com.ridelink.account_service.dto.LoginResponse;
import com.ridelink.account_service.dto.RegisterRequest;
import com.ridelink.account_service.dto.UserResponse;
import com.ridelink.account_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Management", description = "Operations for passenger and driver registration and authentication")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new passenger or driver", description = "Creates a new user account with role PASSENGER or DRIVER, hashes password, and sets status to ACTIVE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email is already registered")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "User login and token issuance", description = "Authenticates user credentials and returns a signed JWT bearer token containing role and account status claims.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, JWT token issued"),
            @ApiResponse(responseCode = "400", description = "Validation error in request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account is suspended or deactivated")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}