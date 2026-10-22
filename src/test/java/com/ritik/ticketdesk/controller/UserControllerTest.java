package com.ritik.ticketdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritik.ticketdesk.dto.UserCreateRequest;
import com.ritik.ticketdesk.dto.UserResponse;
import com.ritik.ticketdesk.entity.UserRole;
import com.ritik.ticketdesk.exception.DuplicateResourceException;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users returns 201 Created on valid input")
    void createUser_Returns201() throws Exception {
        UserCreateRequest request = new UserCreateRequest("Ritik Pal", "ritik@example.com", UserRole.USER);
        UserResponse response = new UserResponse(1L, "Ritik Pal", "ritik@example.com", UserRole.USER, LocalDateTime.now());

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ritik Pal"))
                .andExpect(jsonPath("$.email").value("ritik@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/users returns 409 Conflict when email already exists")
    void createUser_DuplicateEmail_Returns409() throws Exception {
        UserCreateRequest request = new UserCreateRequest("Ritik Pal", "duplicate@example.com", UserRole.USER);

        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("User already exists with email: duplicate@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User already exists with email: duplicate@example.com"))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    @DisplayName("POST /api/users returns 400 Bad Request on blank name or invalid email")
    void createUser_InvalidPayload_Returns400() throws Exception {
        UserCreateRequest request = new UserCreateRequest("", "not-an-email", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 200 OK when user exists")
    void getUserById_Returns200() throws Exception {
        UserResponse response = new UserResponse(5L, "Alice", "alice@example.com", UserRole.ADMIN, LocalDateTime.now());

        when(userService.getUserById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 404 Not Found when user does not exist")
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id 999"))
                .andExpect(jsonPath("$.path").value("/api/users/999"));
    }
}

