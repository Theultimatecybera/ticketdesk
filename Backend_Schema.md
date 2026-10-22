# Backend Schema — TicketDesk API

## 1. Domain model
Entities: `User`, `Ticket`, `Comment`.

Relationships:
- One `User` can create many `Ticket` records.
- One `User` can author many `Comment` records.
- One `Ticket` can contain many `Comment` records.
- A ticket's `created_by` references `users.id`.
- A comment's `ticket_id` references `tickets.id`; `author_id` references `users.id`.

## 2. MySQL schema
```sql
CREATE DATABASE IF NOT EXISTS ticketdesk;
USE ticketdesk;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tickets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL,
  priority VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE comments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  message TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE INDEX idx_tickets_status ON tickets(status);
```

## 3. Enumerations
```java
public enum UserRole {
    USER, ADMIN
}

public enum TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}

public enum Priority {
    LOW, MEDIUM, HIGH
}
```
Persist enums with `@Enumerated(EnumType.STRING)` so the stored values remain readable.

## 4. JPA mapping guidance
- `User`: `@Entity`, unique email, role enum, created timestamp.
- `Ticket`: `@Entity`, title (required, max 200), description, status default `OPEN`, priority, `@ManyToOne(fetch = FetchType.LAZY)` creator, timestamps.
- `Comment`: `@Entity`, required message, lazy `@ManyToOne` ticket and author, timestamp.
- Prefer explicit DTO mapping to prevent entity serialization cycles.
- Use `@Column(nullable = false)` and Bean Validation; database constraints and request validation serve different purposes.

## 5. Repository expectations
- `UserRepository extends JpaRepository<User, Long>`; provide a way to check/find by email.
- `TicketRepository extends JpaRepository<Ticket, Long>`; support status/priority filters and `Pageable`.
- `CommentRepository extends JpaRepository<Comment, Long>`; support listing comments by ticket.
- If optional filters can be combined in arbitrary ways, implement a clear repository strategy (derived query methods, `Specification`, or a custom query) and test each supported combination.

## 6. DTOs (suggested)
- `UserCreateRequest`: `name`, `email`, `role`.
- `UserResponse`: `id`, `name`, `email`, `role`, `createdAt`.
- `TicketRequest`: `title`, `description`, `priority`, `createdBy`.
- `TicketUpdateRequest`: `title`, `description`, `priority`.
- `TicketStatusRequest` or validated query parameter: next status.
- `TicketResponse`: `id`, `title`, `description`, `status`, `priority`, `createdBy`, `createdAt`, `updatedAt`.
- `CommentCreateRequest`: `authorId`, `message`.
- `CommentResponse`: `id`, `ticketId`, `authorId`, `message`, `createdAt`.
- `ApiError`: `timestamp`, `status`, `error`, `message`, `path`.

DTO field names and response details should remain consistent throughout controller, tests, and README.

## 7. Integrity and behavior
- Duplicate email: HTTP 409.
- Unknown user/ticket: HTTP 404.
- Invalid payload or invalid transition: HTTP 400.
- Deleting a ticket cascades to its comments.
- Keep database foreign keys and service-level existence checks.
- Status transition must advance exactly one step; do not rely solely on enum ordinal without tests.
