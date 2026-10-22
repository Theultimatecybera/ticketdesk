package com.ritik.ticketdesk.repository;

import com.ritik.ticketdesk.entity.Priority;
import com.ritik.ticketdesk.entity.Ticket;
import com.ritik.ticketdesk.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByStatusAndPriority(TicketStatus status, Priority priority, Pageable pageable);
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findByPriority(Priority priority, Pageable pageable);
}

