package com.ritik.ticketdesk.repository;

import com.ritik.ticketdesk.entity.Comment;
import com.ritik.ticketdesk.entity.Priority;
import com.ritik.ticketdesk.entity.Ticket;
import com.ritik.ticketdesk.entity.TicketStatus;
import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist ticket with default status OPEN and relationships")
    void shouldPersistTicketWithRelationships() {
        User creator = new User("Jane Doe", "jane@example.com", UserRole.USER);
        entityManager.persistAndFlush(creator);

        Ticket ticket = new Ticket("Network connection issue", "Cannot reach the internal gateway", Priority.HIGH, creator);
        Ticket savedTicket = ticketRepository.save(ticket);

        assertThat(savedTicket.getId()).isNotNull();
        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(savedTicket.getCreatedAt()).isNotNull();
        assertThat(savedTicket.getUpdatedAt()).isNotNull();
        assertThat(savedTicket.getCreatedBy().getId()).isEqualTo(creator.getId());
    }

    @Test
    @DisplayName("Should filter tickets by status and priority with pagination")
    void shouldFilterTicketsByStatusAndPriority() {
        User creator = new User("John Smith", "john@example.com", UserRole.USER);
        entityManager.persistAndFlush(creator);

        Ticket t1 = new Ticket("Ticket One", "Desc 1", Priority.HIGH, creator);
        t1.setStatus(TicketStatus.OPEN);
        entityManager.persist(t1);

        Ticket t2 = new Ticket("Ticket Two", "Desc 2", Priority.LOW, creator);
        t2.setStatus(TicketStatus.OPEN);
        entityManager.persist(t2);

        Ticket t3 = new Ticket("Ticket Three", "Desc 3", Priority.HIGH, creator);
        t3.setStatus(TicketStatus.IN_PROGRESS);
        entityManager.persist(t3);

        entityManager.flush();

        Page<Ticket> openHighTickets = ticketRepository.findByStatusAndPriority(
                TicketStatus.OPEN, Priority.HIGH, PageRequest.of(0, 10));
        assertThat(openHighTickets.getTotalElements()).isEqualTo(1);
        assertThat(openHighTickets.getContent().get(0).getTitle()).isEqualTo("Ticket One");

        Page<Ticket> openTickets = ticketRepository.findByStatus(TicketStatus.OPEN, PageRequest.of(0, 10));
        assertThat(openTickets.getTotalElements()).isEqualTo(2);

        Page<Ticket> highTickets = ticketRepository.findByPriority(Priority.HIGH, PageRequest.of(0, 10));
        assertThat(highTickets.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deleting a ticket should cascade delete its comments")
    void shouldCascadeDeleteCommentsWhenTicketIsDeleted() {
        User author = new User("Alice", "alice@example.com", UserRole.USER);
        entityManager.persistAndFlush(author);

        Ticket ticket = new Ticket("Database outage", "Database is not responding", Priority.HIGH, author);
        entityManager.persistAndFlush(ticket);

        Comment c1 = new Comment(ticket, author, "Looking into this now.");
        Comment c2 = new Comment(ticket, author, "Issue resolved.");
        ticket.getComments().add(c1);
        ticket.getComments().add(c2);
        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.flush();

        List<Comment> commentsBefore = commentRepository.findByTicketId(ticket.getId());
        assertThat(commentsBefore).hasSize(2);

        ticketRepository.delete(ticket);
        entityManager.flush();
        entityManager.clear();

        List<Comment> commentsAfter = commentRepository.findByTicketId(ticket.getId());
        assertThat(commentsAfter).isEmpty();
    }
}
