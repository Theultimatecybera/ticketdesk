package com.ritik.ticketdesk.dto;

import com.ritik.ticketdesk.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class TicketStatusRequest {

    @NotNull(message = "Status is required")
    private TicketStatus status;

    public TicketStatusRequest() {
    }

    public TicketStatusRequest(TicketStatus status) {
        this.status = status;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
