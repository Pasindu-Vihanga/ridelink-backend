package com.ridelink.account_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.exception.GlobalExceptionHandler;
import com.ridelink.account_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = UserResponse.builder()
                .id("u1")
                .fullName("John Doe")
                .email("john@ridelink.com")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "john@ridelink.com",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PASSENGER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getCurrentUser_ReturnsOk() throws Exception {
        when(userService.getProfile("john@ridelink.com")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/users/me").principal(() -> "john@ridelink.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@ridelink.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    void getUserById_ReturnsOk() throws Exception {
        when(userService.getUserById("u1")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"));
    }

    @Test
    void updateProfile_ReturnsOk() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .phoneNumber("+94779999999")
                .build();

        when(userService.updateProfile(eq("john@ridelink.com"), any(UpdateProfileRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/users/profile")
                        .principal(() -> "john@ridelink.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_ReturnsOk() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .build();

        mockMvc.perform(post("/api/users/change-password")
                        .principal(() -> "john@ridelink.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatus_ReturnsOk() throws Exception {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(AccountStatus.SUSPENDED)
                .build();

        sampleResponse.setStatus(AccountStatus.SUSPENDED);
        when(userService.updateStatus(eq("u1"), eq(AccountStatus.SUSPENDED))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/users/u1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void updateRole_ReturnsOk() throws Exception {
        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .role(Role.DRIVER)
                .build();

        sampleResponse.setRole(Role.DRIVER);
        when(userService.updateRole(eq("u1"), eq(Role.DRIVER))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/users/u1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void getAllUsers_ReturnsList() throws Exception {
        List<UserResponse> list = Collections.singletonList(sampleResponse);
        when(userService.getAllUsers(null, null)).thenReturn(list);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void validateUser_ReturnsOk() throws Exception {
        when(userService.validateUser("u1")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/users/u1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
