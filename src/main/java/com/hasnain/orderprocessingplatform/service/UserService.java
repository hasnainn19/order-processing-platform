package com.hasnain.orderprocessingplatform.service;

import com.hasnain.orderprocessingplatform.entity.Role;
import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.repository.UserRepository;
import com.hasnain.orderprocessingplatform.dto.UserResponse;
import com.hasnain.orderprocessingplatform.dto.CreateUserRequest;
import com.hasnain.orderprocessingplatform.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service 
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole()
        );
    }
    
}
