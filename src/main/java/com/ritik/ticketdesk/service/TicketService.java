package com.ritik.ticketdesk.service;

import com.ritik.ticketdesk.dto.PagedResponse;
import com.ritik.ticketdesk.dto.TicketRequest;
import com.ritik.ticketdesk.dto.TicketResponse;
import com.ritik.ticketdesk.dto.TicketUpdateRequest;
import com.ritik.ticketdesk.entity.Priority;
import com.ritik.ticketdesk.entity.Ticket;
import com.ritik.ticketdesk.entity.TicketStatus;
import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.exception.InvalidStatusTransitionException;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.repository.TicketRepository;
import com.ritik.ticketdesk.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public TicketResponse createTicket(TicketRequest request) {
        User creator = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getCreatedBy()));

        Ticket ticket = new Ticket(
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                creator
        );
        ticket.setStatus(TicketStatus.OPEN);

        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));
        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> listTickets(TicketStatus status, Priority priority, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> ticketPage;

        if (status != null && priority != null) {
            ticketPage = ticketRepository.findByStatusAndPriority(status, priority, pageable);
        } else if (status != null) {
            ticketPage = ticketRepository.findByStatus(status, pageable);
        } else if (priority != null) {
            ticketPage = ticketRepository.findByPriority(priority, pageable);
        } else {
            ticketPage = ticketRepository.findAll(pageable);
        }

        Page<TicketResponse> responsePage = ticketPage.map(this::mapToResponse);
        return PagedResponse.from(responsePage);
    }

    public TicketResponse updateTicket(Long id, TicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());

        Ticket updatedTicket = ticketRepository.save(ticket);
        return mapToResponse(updatedTicket);
    }

    public TicketResponse updateStatus(Long id, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));

        if (!isValidStatusTransition(ticket.getStatus(), newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + ticket.getStatus() + " to " + newStatus
            );
        }

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);
        return mapToResponse(updatedTicket);
    }

    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));
        ticketRepository.delete(ticket);
    }

    public boolean isValidStatusTransition(TicketStatus current, TicketStatus next) {
        if (current == null || next == null) {
            return false;
        }
        return switch (current) {
            case OPEN -> next == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED;
            case RESOLVED -> next == TicketStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}

