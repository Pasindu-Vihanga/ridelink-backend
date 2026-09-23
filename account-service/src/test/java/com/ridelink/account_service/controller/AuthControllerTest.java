package com.ridelink.account_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.exception.EmailAlreadyExistsException;
import com.ridelink.account_service.exception.GlobalExceptionHandler;
import com.ridelink.account_service.exception.InvalidCredentialsException;
import com.ridelink.account_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Passenger")
                .email("passenger@ridelink.com")
                .password("securePass123")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .build();

        UserResponse response = UserResponse.builder()
                .id("u100")
                .fullName("John Passenger")
                .email("passenger@ridelink.com")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("u100"))
                .andExpect(jsonPath("$.email").value("passenger@ridelink.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Passenger")
                .email("not-an-email")
                .password("123")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Passenger")
                .email("exists@ridelink.com")
                .password("securePass123")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .build();

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists: exists@ridelink.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("passenger@ridelink.com")
                .password("securePass123")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("mocked.jwt.token")
                .tokenType("Bearer")
                .userId("u100")
                .email("passenger@ridelink.com")
                .fullName("John Passenger")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .expiresIn(86400000L)
                .build();

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("passenger@ridelink.com")
                .password("wrongpassword")
                .build();

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
