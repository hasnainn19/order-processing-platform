package com.hasnain.orderapi.security;

import com.hasnain.orderapi.entity.Role;
import com.hasnain.orderapi.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-jwt-signing-must-be-long-enough-1234567890";

    private User existingUser() {
        User user = new User();
        user.setId(7L);
        user.setEmail("jane@example.com");
        user.setRole(Role.ADMIN);
        return user;
    }

    @Test
    void generateToken_thenExtractedClaims_matchTheOriginalUser() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        User user = existingUser();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_returnsTrue_forFreshlyGeneratedToken() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        String token = jwtService.generateToken(existingUser());

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1_000L);
        String token = jwtService.generateToken(existingUser());

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);

        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
    }
}
