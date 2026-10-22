package com.ritik.ticketdesk.service;

import com.ritik.ticketdesk.dto.CommentCreateRequest;
import com.ritik.ticketdesk.dto.CommentResponse;
import com.ritik.ticketdesk.entity.Comment;
import com.ritik.ticketdesk.entity.Priority;
import com.ritik.ticketdesk.entity.Ticket;
import com.ritik.ticketdesk.entity.TicketStatus;
import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.entity.UserRole;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.repository.CommentRepository;
import com.ritik.ticketdesk.repository.TicketRepository;
import com.ritik.ticketdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private User sampleUser;
    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Ritik", "ritik@example.com", UserRole.USER);
        sampleUser.setId(1L);

        sampleTicket = new Ticket("Payment failing", "Stripe payment times out", Priority.HIGH, sampleUser);
        sampleTicket.setId(10L);
        sampleTicket.setStatus(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("Should successfully add comment to ticket")
    void addComment_Success() {
        CommentCreateRequest request = new CommentCreateRequest(1L, "Investigating payment webhook logs.");

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(100L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        CommentResponse response = commentService.addComment(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTicketId()).isEqualTo(10L);
        assertThat(response.getAuthorId()).isEqualTo(1L);
        assertThat(response.getMessage()).isEqualTo("Investigating payment webhook logs.");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ticket does not exist")
    void addComment_TicketNotFound_ThrowsException() {
        CommentCreateRequest request = new CommentCreateRequest(1L, "Some message");

        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket not found with id 999");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when author does not exist")
    void addComment_AuthorNotFound_ThrowsException() {
        CommentCreateRequest request = new CommentCreateRequest(888L, "Some message");

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        when(userRepository.findById(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(10L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id 888");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should return comments list for existing ticket")
    void getCommentsByTicketId_Success() {
        Comment comment = new Comment(sampleTicket, sampleUser, "Comment text");
        comment.setId(50L);
        comment.setCreatedAt(LocalDateTime.now());

        when(ticketRepository.existsById(10L)).thenReturn(true);
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(comment));

        List<CommentResponse> responses = commentService.getCommentsByTicketId(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(50L);
        assertThat(responses.get(0).getMessage()).isEqualTo("Comment text");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when listing comments for nonexistent ticket")
    void getCommentsByTicketId_TicketNotFound_ThrowsException() {
        when(ticketRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByTicketId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket not found with id 999");
    }
}
