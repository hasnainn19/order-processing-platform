package com.hasnain.orderapi.controller;

import com.hasnain.orderapi.dto.UserResponse;
import com.hasnain.orderapi.entity.Role;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private Authentication authenticatedAsJohn() {
        return new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
    }

    private Authentication authenticatedAsAdmin() {
        return new UsernamePasswordAuthenticationToken("admin@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void getUserById_returnsUser_whenFound() throws Exception {
        when(userService.getUserById(1L, "john@example.com", false))
                .thenReturn(new UserResponse(1L, "john@example.com", Role.USER));

        mockMvc.perform(get("/api/users/1").principal(authenticatedAsJohn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getUserById_returns404_whenUserDoesNotExist() throws Exception {
        when(userService.getUserById(99L, "john@example.com", false))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99").principal(authenticatedAsJohn()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void getUserById_returns403_whenCallerLacksPermission() throws Exception {
        when(userService.getUserById(1L, "john@example.com", false))
                .thenThrow(new AccessDeniedException("You do not have permission to perform this action"));

        mockMvc.perform(get("/api/users/1").principal(authenticatedAsJohn()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void getUserById_passesAdminFlag_whenCallerIsAdmin() throws Exception {
        when(userService.getUserById(1L, "admin@example.com", true))
                .thenReturn(new UserResponse(1L, "john@example.com", Role.USER));

        mockMvc.perform(get("/api/users/1").principal(authenticatedAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createUser_returns200WithCreatedUser_whenRequestIsValid() throws Exception {
        when(userService.createUser(any())).thenReturn(new UserResponse(2L, "jane@example.com", Role.USER));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@example.com\",\"password\":\"plainTextPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void createUser_returns400WithFieldErrors_whenEmailIsInvalidAndPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be at least 8 characters"));
    }
}
