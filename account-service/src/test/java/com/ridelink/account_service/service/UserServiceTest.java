package com.ridelink.account_service.service;

import com.ridelink.account_service.dto.*;
import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.entity.User;
import com.ridelink.account_service.exception.AccountSuspendedException;
import com.ridelink.account_service.exception.BadRequestException;
import com.ridelink.account_service.exception.EmailAlreadyExistsException;
import com.ridelink.account_service.exception.InvalidCredentialsException;
import com.ridelink.account_service.exception.ResourceNotFoundException;
import com.ridelink.account_service.repository.UserRepository;
import com.ridelink.account_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("u1")
                .fullName("John Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("password123")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("password123")
                .phoneNumber("+94771234567")
                .role(Role.PASSENGER)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_AdminRole_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Admin")
                .email("admin@example.com")
                .password("password123")
                .phoneNumber("+94771234567")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.register(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getToken());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "encoded_pass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void login_SuspendedUser_ThrowsException() {
        sampleUser.setStatus(AccountStatus.SUSPENDED);
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        assertThrows(AccountSuspendedException.class, () -> userService.login(request));
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getProfile("john@example.com");

        assertNotNull(response);
        assertEquals("John Doe", response.getFullName());
    }

    @Test
    void getProfile_NotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile("nonexistent@example.com"));
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .phoneNumber("+94779999999")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.updateProfile("john@example.com", request);

        assertNotNull(response);
        assertEquals("John Updated", sampleUser.getFullName());
        assertEquals("+94779999999", sampleUser.getPhoneNumber());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_pass")
                .newPassword("new_pass123")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("old_pass", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.encode("new_pass123")).thenReturn("new_encoded_pass");

        userService.changePassword("john@example.com", request);

        assertEquals("new_encoded_pass", sampleUser.getPassword());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updateStatus_Success() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.updateStatus("u1", AccountStatus.SUSPENDED);

        assertNotNull(response);
        assertEquals(AccountStatus.SUSPENDED, sampleUser.getStatus());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updateRole_Success() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.updateRole("u1", Role.DRIVER);

        assertNotNull(response);
        assertEquals(Role.DRIVER, sampleUser.getRole());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void validateUser_Active_Success() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.validateUser("u1");

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
    }

    @Test
    void validateUser_Suspended_ThrowsException() {
        sampleUser.setStatus(AccountStatus.SUSPENDED);
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser));

        assertThrows(AccountSuspendedException.class, () -> userService.validateUser("u1"));
    }

    @Test
    void getAllUsers_Filters() {
        when(userRepository.findByRoleAndStatus(Role.PASSENGER, AccountStatus.ACTIVE))
                .thenReturn(Collections.singletonList(sampleUser));

        List<UserResponse> list = userService.getAllUsers(Role.PASSENGER, AccountStatus.ACTIVE);

        assertEquals(1, list.size());
        assertEquals("john@example.com", list.get(0).getEmail());
    }
}
