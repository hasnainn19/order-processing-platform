package com.hasnain.orderapi.service;

import com.hasnain.orderapi.dto.LoginRequest;
import com.hasnain.orderapi.dto.LoginResponse;
import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.exception.InvalidCredentialsException;
import com.hasnain.orderapi.repository.UserRepository;
import com.hasnain.orderapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setPasswordHash("hashed-value");
        return user;
    }

    @Test
    void login_returnsGeneratedToken_whenCredentialsAreValid() {
        User user = existingUser();
        LoginRequest request = new LoginRequest("jane@example.com", "correctPassword");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "hashed-value")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed-jwt");
    }

    @Test
    void login_throwsInvalidCredentialsException_andNeverGeneratesToken_whenEmailNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("ghost@example.com", "anyPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_throwsInvalidCredentialsException_andNeverGeneratesToken_whenPasswordDoesNotMatch() {
        User user = existingUser();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed-value")).thenReturn(false);

        LoginRequest request = new LoginRequest("jane@example.com", "wrongPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
