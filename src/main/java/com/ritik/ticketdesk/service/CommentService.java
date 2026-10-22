package com.ritik.ticketdesk.service;

import com.ritik.ticketdesk.dto.CommentCreateRequest;
import com.ritik.ticketdesk.dto.CommentResponse;
import com.ritik.ticketdesk.entity.Comment;
import com.ritik.ticketdesk.entity.Ticket;
import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.exception.ResourceNotFoundException;
import com.ritik.ticketdesk.repository.CommentRepository;
import com.ritik.ticketdesk.repository.TicketRepository;
import com.ritik.ticketdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public CommentResponse addComment(Long ticketId, CommentCreateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + ticketId));

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getAuthorId()));

        Comment comment = new Comment(ticket, author, request.getMessage());
        Comment savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTicketId(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with id " + ticketId);
        }

        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse mapToResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket() != null ? comment.getTicket().getId() : null,
                comment.getAuthor() != null ? comment.getAuthor().getId() : null,
                comment.getMessage(),
                comment.getCreatedAt()
        );
    }
}
