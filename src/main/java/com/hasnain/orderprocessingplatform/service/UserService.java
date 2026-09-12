package com.hasnain.orderprocessingplatform.service;

import com.hasnain.orderprocessingplatform.entity.Role;
import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.repository.UserRepository;
import com.hasnain.orderprocessingplatform.dto.UserResponse;
import com.hasnain.orderprocessingplatform.dto.CreateUserRequest;
import org.springframework.stereotype.Service;

@Service 
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(request.password()); // TODO: replace with hashed password
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
