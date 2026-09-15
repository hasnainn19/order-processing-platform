package com.hasnain.orderapi.dto;

import com.hasnain.orderapi.entity.Role;

public record UserResponse(
    Long id,
    String email,
    Role role
) {}
