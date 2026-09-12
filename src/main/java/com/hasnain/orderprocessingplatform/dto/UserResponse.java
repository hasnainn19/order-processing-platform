package com.hasnain.orderprocessingplatform.dto;

import com.hasnain.orderprocessingplatform.entity.Role;

public record UserResponse(
    Long id,
    String email,
    Role role
) {}
