package com.ritik.ticketdesk.service;

import com.ritik.ticketdesk.dto.UserCreateRequest;
import com.ritik.ticketdesk.dto.UserResponse;
import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.entity.UserRole;
import com.ritik.ticketdesk.exception.DuplicateResourceException;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should successfully create a user when email is unique")
    void createUser_Success() {
        UserCreateRequest request = new UserCreateRequest("Ritik Pal", "ritik@example.com", UserRole.USER);

        when(userRepository.existsByEmail("ritik@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Ritik Pal");
        assertThat(response.getEmail()).isEqualTo("ritik@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void createUser_DuplicateEmail_ThrowsDuplicateResourceException() {
        UserCreateRequest request = new UserCreateRequest("Duplicate User", "duplicate@example.com", UserRole.USER);

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User already exists with email: duplicate@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should retrieve existing user by ID")
    void getUserById_Success() {
        User user = new User("Alice", "alice@example.com", UserRole.ADMIN);
        user.setId(5L);
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(5L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not found")
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id 999");
    }
}
