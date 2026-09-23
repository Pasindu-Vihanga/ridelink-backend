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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Self-registration as ADMIN is not permitted");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber().trim())
                .role(request.getRole())
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromUser(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException("Account has been suspended. Please contact support.");
        }

        if (user.getStatus() == AccountStatus.DEACTIVATED) {
            throw new AccountSuspendedException("Account has been deactivated. Please contact support to reactivate.");
        }

        String token = jwtService.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponse.fromUser(user);
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromUser(user);
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserResponse updateStatus(String id, AccountStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(user);
        return UserResponse.fromUser(updated);
    }

    public UserResponse updateRole(String id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(user);
        return UserResponse.fromUser(updated);
    }

    public List<UserResponse> getAllUsers(Role role, AccountStatus status) {
        List<User> users;
        if (role != null && status != null) {
            users = userRepository.findByRoleAndStatus(role, status);
        } else if (role != null) {
            users = userRepository.findByRole(role);
        } else if (status != null) {
            users = userRepository.findByStatus(status);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    public UserResponse validateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountSuspendedException("User account is not ACTIVE (current status: " + user.getStatus() + ")");
        }

        return UserResponse.fromUser(user);
    }

    public void deactivateOwnAccount(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setStatus(AccountStatus.DEACTIVATED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}