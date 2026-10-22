package com.ritik.ticketdesk.service;

import com.ritik.ticketdesk.dto.PagedResponse;
import com.ritik.ticketdesk.dto.TicketRequest;
import com.ritik.ticketdesk.dto.TicketResponse;
import com.ritik.ticketdesk.dto.TicketUpdateRequest;
import com.ritik.ticketdesk.entity.Priority;
import com.ritik.ticketdesk.entity.Ticket;
import com.ritik.ticketdesk.entity.TicketStatus;
import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.entity.UserRole;
import com.ritik.ticketdesk.exception.InvalidStatusTransitionException;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.repository.TicketRepository;
import com.ritik.ticketdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    private User sampleUser;
    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Ritik", "ritik@example.com", UserRole.USER);
        sampleUser.setId(1L);

        sampleTicket = new Ticket("VPN connection failing", "Cannot connect to the office VPN", Priority.HIGH, sampleUser);
        sampleTicket.setId(10L);
        sampleTicket.setStatus(TicketStatus.OPEN);
        sampleTicket.setCreatedAt(LocalDateTime.now());
        sampleTicket.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create ticket with status OPEN when creator exists")
    void createTicket_Success() {
        TicketRequest request = new TicketRequest("VPN connection failing", "Cannot connect to the office VPN", Priority.HIGH, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(10L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("VPN connection failing");
        assertThat(response.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getCreatedBy()).isEqualTo(1L);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating ticket with nonexistent creator")
    void createTicket_CreatorNotFound_ThrowsResourceNotFoundException() {
        TicketRequest request = new TicketRequest("Broken printer", "Office printer 2 is jamming", Priority.LOW, 999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id 999");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should get ticket by ID successfully")
    void getTicketById_Success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

        TicketResponse response = ticketService.getTicketById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("VPN connection failing");
        assertThat(response.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ticket ID does not exist")
    void getTicketById_NotFound_ThrowsResourceNotFoundException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket not found with id 99");
    }

    @Test
    @DisplayName("Should update ticket title, description, and priority")
    void updateTicket_Success() {
        TicketUpdateRequest updateRequest = new TicketUpdateRequest("Updated title", "Updated description", Priority.MEDIUM);

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.updateTicket(10L, updateRequest);

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @ParameterizedTest(name = "Valid transition from {0} to {1}")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED"
    })
    @DisplayName("Should successfully advance status along exact single-step workflow")
    void updateStatus_ValidTransitions_Success(TicketStatus initial, TicketStatus next) {
        sampleTicket.setStatus(initial);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.updateStatus(10L, next);

        assertThat(response.getStatus()).isEqualTo(next);
    }

    @ParameterizedTest(name = "Invalid transition from {0} to {1}")
    @CsvSource({
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "OPEN, OPEN",
            "IN_PROGRESS, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, IN_PROGRESS",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, OPEN",
            "RESOLVED, RESOLVED",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "CLOSED, CLOSED"
    })
    @DisplayName("Should reject skipped, backward, repeated or post-closed transitions with InvalidStatusTransitionException")
    void updateStatus_InvalidTransitions_ThrowsException(TicketStatus initial, TicketStatus target) {
        sampleTicket.setStatus(initial);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

        assertThatThrownBy(() -> ticketService.updateStatus(10L, target))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition from " + initial + " to " + target);

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should delete ticket when ticket exists")
    void deleteTicket_Success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

        ticketService.deleteTicket(10L);

        verify(ticketRepository).delete(sampleTicket);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting nonexistent ticket")
    void deleteTicket_NotFound_ThrowsException() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.deleteTicket(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket not found with id 999");

        verify(ticketRepository, never()).delete(any(Ticket.class));
    }

    @Test
    @DisplayName("Should list tickets with status and priority filters")
    void listTickets_WithFilters_Success() {
        when(ticketRepository.findByStatusAndPriority(eq(TicketStatus.OPEN), eq(Priority.HIGH), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTicket)));

        PagedResponse<TicketResponse> response = ticketService.listTickets(TicketStatus.OPEN, Priority.HIGH, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("VPN connection failing");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject negative page index")
    void listTickets_NegativePage_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> ticketService.listTickets(null, null, -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page index must not be less than zero");
    }

    @Test
    @DisplayName("Should reject page size less than 1 or greater than 100")
    void listTickets_InvalidSize_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> ticketService.listTickets(null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be between 1 and 100");

        assertThatThrownBy(() -> ticketService.listTickets(null, null, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be between 1 and 100");
    }
}
