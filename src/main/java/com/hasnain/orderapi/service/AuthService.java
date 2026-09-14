package com.hasnain.orderapi.service;

import org.springframework.stereotype.Service;

import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.exception.InvalidCredentialsException;
import com.hasnain.orderapi.repository.UserRepository;
import com.hasnain.orderapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.hasnain.orderapi.dto.LoginRequest;
import com.hasnain.orderapi.dto.LoginResponse;

@Service 
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token);
    }
}
