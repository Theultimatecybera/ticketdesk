package com.ritik.ticketdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritik.ticketdesk.dto.PagedResponse;
import com.ritik.ticketdesk.dto.TicketRequest;
import com.ritik.ticketdesk.dto.TicketResponse;
import com.ritik.ticketdesk.dto.TicketStatusRequest;
import com.ritik.ticketdesk.dto.TicketUpdateRequest;
import com.ritik.ticketdesk.entity.Priority;
import com.ritik.ticketdesk.entity.TicketStatus;
import com.ritik.ticketdesk.exception.InvalidStatusTransitionException;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.service.TicketService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @Test
    @DisplayName("POST /api/tickets returns 201 Created on valid payload")
    void createTicket_Returns201() throws Exception {
        TicketRequest request = new TicketRequest("Login page failing", "Blank screen upon submit", Priority.HIGH, 1L);
        TicketResponse response = new TicketResponse(10L, "Login page failing", "Blank screen upon submit",
                TicketStatus.OPEN, Priority.HIGH, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(ticketService.createTicket(any(TicketRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Login page failing"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.createdBy").value(1));
    }

    @Test
    @DisplayName("POST /api/tickets returns 400 Bad Request when title is less than 5 characters")
    void createTicket_TitleTooShort_Returns400() throws Exception {
        TicketRequest request = new TicketRequest("Bug", "Short title", Priority.LOW, 1L);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Title must be between 5 and 200 characters")));
    }

    @Test
    @DisplayName("POST /api/tickets returns 404 Not Found when creator does not exist")
    void createTicket_CreatorNotFound_Returns404() throws Exception {
        TicketRequest request = new TicketRequest("Valid Title Here", "Valid description", Priority.LOW, 999L);

        when(ticketService.createTicket(any(TicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id 999"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id 999"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} returns 200 OK when ticket exists")
    void getTicketById_Returns200() throws Exception {
        TicketResponse response = new TicketResponse(10L, "Sample title", "Sample description",
                TicketStatus.OPEN, Priority.MEDIUM, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(ticketService.getTicketById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/tickets/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Sample title"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} returns 404 Not Found when ticket does not exist")
    void getTicketById_NotFound_Returns404() throws Exception {
        when(ticketService.getTicketById(99L))
                .thenThrow(new ResourceNotFoundException("Ticket not found with id 99"));

        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found with id 99"));
    }

    @Test
    @DisplayName("PUT /api/tickets/{id} returns 200 OK when updating details")
    void updateTicket_Returns200() throws Exception {
        TicketUpdateRequest updateRequest = new TicketUpdateRequest("Updated title text", "New description", Priority.LOW);
        TicketResponse response = new TicketResponse(10L, "Updated title text", "New description",
                TicketStatus.OPEN, Priority.LOW, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(ticketService.updateTicket(eq(10L), any(TicketUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tickets/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title text"))
                .andExpect(jsonPath("$.priority").value("LOW"));
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id}/status returns 200 OK on valid status step")
    void updateStatus_Valid_Returns200() throws Exception {
        TicketStatusRequest statusRequest = new TicketStatusRequest(TicketStatus.IN_PROGRESS);
        TicketResponse response = new TicketResponse(10L, "Title", "Desc",
                TicketStatus.IN_PROGRESS, Priority.HIGH, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(ticketService.updateStatus(10L, TicketStatus.IN_PROGRESS)).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id}/status returns 400 Bad Request on invalid transition")
    void updateStatus_InvalidTransition_Returns400() throws Exception {
        TicketStatusRequest statusRequest = new TicketStatusRequest(TicketStatus.CLOSED);

        when(ticketService.updateStatus(10L, TicketStatus.CLOSED))
                .thenThrow(new InvalidStatusTransitionException("Invalid status transition from OPEN to CLOSED"));

        mockMvc.perform(patch("/api/tickets/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid status transition from OPEN to CLOSED"))
                .andExpect(jsonPath("$.path").value("/api/tickets/10/status"));
    }

    @Test
    @DisplayName("DELETE /api/tickets/{id} returns 204 No Content on successful deletion")
    void deleteTicket_Returns204() throws Exception {
        doNothing().when(ticketService).deleteTicket(10L);

        mockMvc.perform(delete("/api/tickets/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/tickets/{id} returns 404 Not Found when ticket missing")
    void deleteTicket_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Ticket not found with id 999"))
                .when(ticketService).deleteTicket(999L);

        mockMvc.perform(delete("/api/tickets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/tickets returns 200 with paginated tickets")
    void listTickets_Returns200() throws Exception {
        TicketResponse response = new TicketResponse(10L, "Sample title", "Sample description",
                TicketStatus.OPEN, Priority.MEDIUM, 1L, LocalDateTime.now(), LocalDateTime.now());
        PagedResponse<TicketResponse> pagedResponse = new PagedResponse<>(List.of(response), 0, 10, 1L, 1, true);

        when(ticketService.listTickets(null, null, 0, 10)).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/tickets?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }
}
