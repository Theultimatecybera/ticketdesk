package com.ritik.ticketdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritik.ticketdesk.dto.CommentCreateRequest;
import com.ritik.ticketdesk.dto.CommentResponse;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("POST /api/tickets/{ticketId}/comments returns 201 Created on valid input")
    void addComment_Returns201() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest(1L, "Working on the fix now.");
        CommentResponse response = new CommentResponse(100L, 10L, 1L, "Working on the fix now.", LocalDateTime.now());

        when(commentService.addComment(eq(10L), any(CommentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickets/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.ticketId").value(10))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.message").value("Working on the fix now."));
    }

    @Test
    @DisplayName("POST /api/tickets/{ticketId}/comments returns 400 Bad Request on blank message")
    void addComment_BlankMessage_Returns400() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest(1L, "");

        mockMvc.perform(post("/api/tickets/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Message is required")));
    }

    @Test
    @DisplayName("POST /api/tickets/{ticketId}/comments returns 404 Not Found when ticket does not exist")
    void addComment_TicketNotFound_Returns404() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest(1L, "Valid comment message");

        when(commentService.addComment(eq(999L), any(CommentCreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ticket not found with id 999"));

        mockMvc.perform(post("/api/tickets/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found with id 999"));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketId}/comments returns 200 OK with comment list")
    void getCommentsByTicketId_Returns200() throws Exception {
        CommentResponse response = new CommentResponse(100L, 10L, 1L, "Initial observation.", LocalDateTime.now());

        when(commentService.getCommentsByTicketId(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tickets/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].message").value("Initial observation."));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketId}/comments returns 404 Not Found when ticket does not exist")
    void getCommentsByTicketId_TicketNotFound_Returns404() throws Exception {
        when(commentService.getCommentsByTicketId(999L))
                .thenThrow(new ResourceNotFoundException("Ticket not found with id 999"));

        mockMvc.perform(get("/api/tickets/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found with id 999"));
    }
}

