package com.hasnain.orderapi.controller;

import com.hasnain.orderapi.service.UserService;

import jakarta.validation.Valid;

import com.hasnain.orderapi.dto.UserResponse;
import com.hasnain.orderapi.dto.CreateUserRequest;
import com.hasnain.orderapi.security.SecurityUtils;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController 
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id, Authentication authentication) {
        return userService.getUserById(id, authentication.getName(), SecurityUtils.isAdmin(authentication));
    }

    @PostMapping
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request);
    }
}
