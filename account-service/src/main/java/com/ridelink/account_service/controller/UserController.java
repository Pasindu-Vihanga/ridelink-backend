package com.ridelink.account_service.controller;

import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.exception.InvalidCredentialsException;
import com.ridelink.account_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User & Profile Management", description = "Endpoints for user profile viewing, updating, role management, and account status")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    })
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String email = principal.getName();
        UserResponse response = userService.getProfile(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserResponse> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (principal == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String email = principal.getName();
        UserResponse response = userService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for authenticated user", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid current password"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<com.ridelink.account_service.dto.ApiResponse> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (principal == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String email = principal.getName();
        userService.changePassword(email, request);
        return ResponseEntity.ok(com.ridelink.account_service.dto.ApiResponse.builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate current user account", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<com.ridelink.account_service.dto.ApiResponse> deactivateAccount(Principal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String email = principal.getName();
        userService.deactivateOwnAccount(email);
        return ResponseEntity.ok(com.ridelink.account_service.dto.ApiResponse.builder()
                .success(true)
                .message("Account has been deactivated")
                .build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user account status (Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account status updated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        UserResponse response = userService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User role updated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UserResponse response = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users with optional role and status filters (Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status
    ) {
        List<UserResponse> response = userService.getAllUsers(role, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/validate")
    @Operation(summary = "Interservice validation of an active user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User is valid and ACTIVE"),
            @ApiResponse(responseCode = "403", description = "User is not active (SUSPENDED or DEACTIVATED)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> validateUser(@PathVariable String id) {
        UserResponse response = userService.validateUser(id);
        return ResponseEntity.ok(response);
    }
}