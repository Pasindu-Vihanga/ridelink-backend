package com.ridelink.account_service.security;

import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "mySuperSecretKeyForRideLinkJWTAuthentication2026VerySecureKeyRideLink2026");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void testGenerateTokenAndExtractClaims() {
        User user = User.builder()
                .id("user123")
                .email("test@ridelink.com")
                .fullName("John Doe")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .build();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("test@ridelink.com", jwtService.extractEmail(token));
        assertEquals("PASSENGER", jwtService.extractRole(token));
        assertEquals("user123", jwtService.extractUserId(token));
        assertTrue(jwtService.isTokenValid(token, "test@ridelink.com"));
        assertFalse(jwtService.isTokenValid(token, "wrong@ridelink.com"));
        assertFalse(jwtService.isTokenExpired(token));
    }
}
