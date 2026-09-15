package com.hasnain.orderapi.service;

import com.hasnain.orderapi.dto.CreateUserRequest;
import com.hasnain.orderapi.dto.UserResponse;
import com.hasnain.orderapi.entity.Role;
import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_returnsMappedResponse_whenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setEmail("john@example.com");
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void getUserById_throwsResourceNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createUser_savesHashedPasswordAndUserRole_neverThePlainPassword() {
        CreateUserRequest request = new CreateUserRequest("jane@example.com", "plainTextPassword123");

        when(passwordEncoder.encode("plainTextPassword123")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-value");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("plainTextPassword123");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.email()).isEqualTo("jane@example.com");
    }
}
