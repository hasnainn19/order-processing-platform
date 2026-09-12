package com.hasnain.orderprocessingplatform.service;

import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service 
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }
    
}
