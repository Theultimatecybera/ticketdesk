package com.ritik.ticketdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CommentCreateRequest {

    @NotNull(message = "Author ID is required")
    private Long authorId;

    @NotBlank(message = "Message is required")
    private String message;

    public CommentCreateRequest() {
    }

    public CommentCreateRequest(Long authorId, String message) {
        this.authorId = authorId;
        this.message = message;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

