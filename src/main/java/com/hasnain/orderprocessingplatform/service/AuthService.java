package com.hasnain.orderprocessingplatform.service;

import org.springframework.stereotype.Service;

import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.exception.InvalidCredentialsException;
import com.hasnain.orderprocessingplatform.repository.UserRepository;
import com.hasnain.orderprocessingplatform.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.hasnain.orderprocessingplatform.dto.LoginRequest;
import com.hasnain.orderprocessingplatform.dto.LoginResponse;

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
