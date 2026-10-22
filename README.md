# TicketDesk API

TicketDesk is a production-style, portfolio-ready REST API for support ticket management built with **Java 17 (LTS)** and **Spring Boot 3.3.5**, backed by **MySQL 8** (with **H2** in-memory database for automated testing).

It demonstrates clean layered architecture, separation of concerns, explicit request/response Data Transfer Objects (DTOs), constructor dependency injection, Jakarta Bean Validation, centralized exception handling with consistent JSON error envelopes, an OpenAPI/Swagger interactive UI, and an automated test suite comprising **62 passing tests** with **92.7% service-layer line coverage**.

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [Architecture & Design Principles](#architecture--design-principles)
- [Package Layout](#package-layout)
- [Status Workflow Rule](#status-workflow-rule)
- [API Endpoints](#api-endpoints)
- [Consistent Error Response](#consistent-error-response)
- [Prerequisites & Local Setup](#prerequisites--local-setup)
- [Configuration & Environment Variables](#configuration--environment-variables)
- [Running the Application & Tests](#running-the-application--tests)
- [Swagger UI & API Docs](#swagger-ui--api-docs)
- [Postman Collection](#postman-collection)
- [Automated Test Suite & Verified Coverage](#automated-test-suite--verified-coverage)
- [Interview & Architectural Q&A](#interview--architectural-qa)

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Spring Validation) |
| Database | MySQL 8 / MariaDB (wire-compatible) |
| Test Database | H2 In-Memory Database (`jdbc:h2:mem:ticketdesk_test`) |
| Build Tool | Apache Maven 3.9+ (includes `mvnw` / `mvnw.cmd` wrapper) |
| Testing | JUnit 5 (Jupiter), Mockito, Spring MockMvc, AssertJ |
| Documentation | springdoc-openapi 2.6.0 (Swagger UI & OpenAPI 3.0) |
| Code Coverage | JaCoCo 0.8.12 |

---

## Architecture & Design Principles

TicketDesk follows a strict 4-tier layered architecture:

```
[HTTP Client / Postman / Swagger]
               │
               ▼
   [REST Controller Layer]        -> Request binding, @Valid validation, HTTP status codes
               │
               ▼
      [Service Layer]             -> Business logic, domain rules, transaction boundaries
               │
               ▼
     [Repository Layer]           -> Spring Data JPA persistence interfaces
               │
               ▼
    [Relational Database]         -> MySQL 8 (production / local) or H2 (automated tests)
```

1. **Thin Controllers:** Controllers only bind HTTP requests, trigger Bean Validation, delegate processing to services, and map to appropriate HTTP status codes (`201 Created`, `200 OK`, `204 No Content`). Controllers never interact directly with JPA repositories.
2. **Business Logic in Services:** All domain validation, existence checks, email uniqueness verification, and status transition workflows reside within the service layer.
3. **No Direct Entity Exposure:** Entities (`User`, `Ticket`, `Comment`) are internal persistence details. All external endpoints consume request DTOs (`UserCreateRequest`, `TicketRequest`, `CommentCreateRequest`) and return immutable response DTOs (`UserResponse`, `TicketResponse`, `CommentResponse`).
4. **Constructor Dependency Injection:** All components utilize constructor injection instead of field injection (`@Autowired`), facilitating unit testing without Spring container overhead.
5. **Centralized Error Handling:** All runtime exceptions are captured by a `@RestControllerAdvice` global exception handler that returns a consistent JSON `ApiError` payload without exposing internal stack traces.

---

## Package Layout

```text
com.ritik.ticketdesk
├── config
│   └── OpenApiConfig.java           # OpenAPI metadata and Swagger UI setup
├── controller
│   ├── CommentController.java       # Ticket discussion endpoints (/api/tickets/{id}/comments)
│   ├── TicketController.java        # Ticket CRUD & status workflow (/api/tickets)
│   └── UserController.java          # User creation and lookup (/api/users)
├── dto
│   ├── ApiError.java                # Standard error response envelope
│   ├── CommentCreateRequest.java    # Comment creation payload
│   ├── CommentResponse.java         # Comment response transfer object
│   ├── PagedResponse.java           # Paginated collection wrapper
│   ├── TicketRequest.java           # Ticket creation payload
│   ├── TicketResponse.java          # Ticket response transfer object
│   ├── TicketStatusRequest.java     # Ticket status change payload
│   ├── TicketUpdateRequest.java     # Ticket details update payload
│   ├── UserCreateRequest.java       # User registration payload
│   └── UserResponse.java            # User response transfer object
├── entity
│   ├── Comment.java                 # JPA Entity for comments table
│   ├── Priority.java                # Enum: LOW, MEDIUM, HIGH
│   ├── Ticket.java                  # JPA Entity for tickets table
│   ├── TicketStatus.java            # Enum: OPEN, IN_PROGRESS, RESOLVED, CLOSED
│   ├── User.java                    # JPA Entity for users table
│   └── UserRole.java                # Enum: USER, ADMIN
├── exception
│   ├── DuplicateResourceException.java         # Mapped to HTTP 409 Conflict
│   ├── GlobalExceptionHandler.java             # @RestControllerAdvice handler
│   ├── InvalidStatusTransitionException.java   # Mapped to HTTP 400 Bad Request
│   └── ResourceNotFoundException.java          # Mapped to HTTP 404 Not Found
├── repository
│   ├── CommentRepository.java       # Spring Data JPA repository for comments
│   ├── TicketRepository.java        # Spring Data JPA repository with filtering & paging
│   └── UserRepository.java          # Spring Data JPA repository with email checks
└── TicketDeskApplication.java       # Application entry point
```

---

## Status Workflow Rule

TicketDesk enforces a strict forward-only, one-step ticket status lifecycle:

```
[OPEN] ───► [IN_PROGRESS] ───► [RESOLVED] ───► [CLOSED]
```

- **Valid Transitions:**
  - `OPEN` ➔ `IN_PROGRESS`
  - `IN_PROGRESS` ➔ `RESOLVED`
  - `RESOLVED` ➔ `CLOSED`
- **Invalid Transitions (Return HTTP 400 Bad Request):**
  - **Skipping steps:** `OPEN` ➔ `RESOLVED`, `OPEN` ➔ `CLOSED`, `IN_PROGRESS` ➔ `CLOSED`
  - **Moving backwards:** `RESOLVED` ➔ `IN_PROGRESS`, `CLOSED` ➔ `OPEN`, etc.
  - **Repeating current state:** `OPEN` ➔ `OPEN`, `CLOSED` ➔ `CLOSED`, etc.
  - **Transitioning after closed:** `CLOSED` cannot transition to any status.

---

## API Endpoints

### 1. Users

| Method | Endpoint | Description | Request Body | Success Code | Error Codes |
|---|---|---|---|---|---|
| `POST` | `/api/users` | Create a new user | `UserCreateRequest` | `201 Created` | `400 Bad Request`, `409 Conflict` |
| `GET` | `/api/users/{id}` | Get user by ID | None | `200 OK` | `404 Not Found` |

### 2. Tickets

| Method | Endpoint | Description | Query / Request Body | Success Code | Error Codes |
|---|---|---|---|---|---|
| `POST` | `/api/tickets` | Create a ticket (starts in `OPEN`) | `TicketRequest` | `201 Created` | `400 Bad Request`, `404 Not Found` |
| `GET` | `/api/tickets` | List tickets with filters & pagination | `status`, `priority`, `page`, `size` | `200 OK` | `400 Bad Request` |
| `GET` | `/api/tickets/{id}` | Get ticket by ID | None | `200 OK` | `404 Not Found` |
| `PUT` | `/api/tickets/{id}` | Update title, description, priority | `TicketUpdateRequest` | `200 OK` | `400 Bad Request`, `404 Not Found` |
| `PATCH` | `/api/tickets/{id}/status` | Advance status by 1 step | `TicketStatusRequest` | `200 OK` | `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/tickets/{id}` | Delete ticket (cascades to comments) | None | `204 No Content` | `404 Not Found` |

### 3. Comments

| Method | Endpoint | Description | Request Body | Success Code | Error Codes |
|---|---|---|---|---|---|
| `POST` | `/api/tickets/{id}/comments` | Add comment to ticket | `CommentCreateRequest` | `201 Created` | `400 Bad Request`, `404 Not Found` |
| `GET` | `/api/tickets/{id}/comments` | List comments for ticket (oldest first) | None | `200 OK` | `404 Not Found` |

---

## Consistent Error Response

All client, validation, business rule, and server errors return a standard JSON structure defined by `ApiError`:

```json
{
  "timestamp": "2026-10-22T19:50:00",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id 99",
  "path": "/api/tickets/99"
}
```

### Validation Error Example (HTTP 400)
When a client submits invalid fields:
```json
{
  "timestamp": "2026-10-22T19:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "title: Title must be between 5 and 200 characters; priority: Priority is required",
  "path": "/api/tickets"
}
```

### Duplicate Email Example (HTTP 409)
```json
{
  "timestamp": "2026-10-22T19:50:00",
  "status": 409,
  "error": "Conflict",
  "message": "User already exists with email: ritik.pal@example.com",
  "path": "/api/users"
}
```

### Invalid Status Transition Example (HTTP 400)
```json
{
  "timestamp": "2026-10-22T19:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid status transition from OPEN to CLOSED",
  "path": "/api/tickets/1/status"
}
```

---

## Prerequisites & Local Setup

- **Java Development Kit (JDK):** Version 17 (Eclipse Temurin 17 or Oracle OpenJDK 17)
- **Database:** MySQL 8 or MariaDB Server (port `3306`)
- **Maven:** 3.9+ (or use the included `./mvnw` / `mvnw.cmd`)
- **Git**

### Step 1: Create the Database
In your MySQL/MariaDB client:
```sql
CREATE DATABASE IF NOT EXISTS ticketdesk;
```

---

## Configuration & Environment Variables

TicketDesk uses environment variables for sensitive settings. The database password is never hardcoded.

In `src/main/resources/application.properties`:
```properties
spring.application.name=ticketdesk
spring.datasource.url=jdbc:mysql://localhost:3306/ticketdesk
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

To set `DB_PASSWORD`:
- **Windows (PowerShell):**
  ```powershell
  $env:DB_PASSWORD="your_password"
  ```
- **macOS / Linux:**
  ```bash
  export DB_PASSWORD="your_password"
  ```
*(If your local root account has no password, you can leave `DB_PASSWORD` unset or empty).*

---

## Running the Application & Tests

### Execute the Automated Test Suite
All tests execute against the isolated in-memory H2 database with zero external dependencies:
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### Run the Application Locally
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Once started, the API listens on port `8080`.

---

## Swagger UI & API Docs

When the application is running:
- **Interactive Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Raw OpenAPI 3.0 JSON Specification:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Postman Collection

A ready-to-import Postman collection is located in the root directory:
- [`TicketDesk.postman_collection.json`](./TicketDesk.postman_collection.json)

Import it directly into Postman to test the complete user creation, ticket lifecycle, comments, pagination, and error scenarios.

---

## Automated Test Suite & Verified Coverage

All tests are verified through Maven Surefire and JaCoCo:

```
[INFO] Results:
[INFO] Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Breakdown of 62 Automated Tests:
1. **Controller Tests (`@WebMvcTest` + MockMvc): 21 tests**
   - `UserControllerTest`: 5 tests covering `201 Created`, `409 Conflict`, `400 Bad Request` validation, `200 OK`, `404 Not Found`.
   - `TicketControllerTest`: 11 tests covering CRUD, short title validation, missing creator, valid & invalid status transitions, pagination response, deletion.
   - `CommentControllerTest`: 5 tests covering comment creation, missing comment message, missing ticket, and listing comments.
2. **Service Unit Tests (JUnit 5 + Mockito): 35 tests**
   - `TicketServiceTest`: 26 tests covering ticket creation, status validation, all valid transitions (`OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`), all invalid transitions (skips, backwards, repeated), negative pagination bounds, and deletion.
   - `CommentServiceTest`: 5 tests covering comment creation, missing ticket/author, and list retrieval.
   - `UserServiceTest`: 4 tests covering unique email creation, duplicate email exception, lookup by ID, and missing user.
3. **Repository Tests (`@DataJpaTest` + H2): 5 tests**
   - `TicketRepositoryTest`: 3 tests verifying entity persistence, relationship mappings, status & priority composite filtering with pagination, and cascade comment deletion on ticket delete.
   - `UserRepositoryTest`: 2 tests verifying user persistence and email uniqueness index lookup.
4. **Integration Context Test: 1 test**
   - `TicketDeskApplicationTests`: Verifies complete Spring Boot application context bootstrap.

### Measured Service Layer JaCoCo Code Coverage:
- **TicketService:** 91.0% line coverage (61/67 lines covered, 87.1% instructions)
- **CommentService:** 100.0% line coverage (23/23 lines covered, 98.2% instructions)
- **UserService:** 89.5% line coverage (17/19 lines covered, 82.0% instructions)
- **Total Service Layer:** **92.7% line coverage** (101/109 lines covered), far exceeding the 70% target.

---

## Interview & Architectural Q&A

### 1. Why use DTOs instead of exposing JPA Entities directly?
Exposing entities couples the database schema to the API contract, risks circular serialization issues (`InfiniteRecursion` in bidirectional relationships), inadvertently exposes sensitive fields, and causes lazy-loading exceptions (`LazyInitializationException`) outside the transaction boundary. DTOs provide clean, intentional, and decoupled API contracts.

### 2. Why Constructor Dependency Injection over `@Autowired` field injection?
- Enforces immutability via `final` fields.
- Prevents `NullPointerException` by guaranteeing all required dependencies are provided upon instantiation.
- Allows simple instantiation and mocking in unit tests without starting a Spring application context or reflection hacks.

### 3. How is the Ticket Status Workflow enforced?
The status transition is validated in `TicketService.updateStatus()` using a strict switch mapping before persisting. Only the exact immediate next step (`OPEN` ➔ `IN_PROGRESS` ➔ `RESOLVED` ➔ `CLOSED`) is accepted. Any attempt to skip steps, reverse order, or re-apply an identical state throws `InvalidStatusTransitionException`, returning HTTP `400 Bad Request`.

### 4. How are JPA relationships configured?
- `Ticket.createdBy` and `Comment.author` use `@ManyToOne(fetch = FetchType.LAZY)`. Lazy loading avoids unintended `EAGER` joins that degrade query performance.
- `Ticket.comments` uses `@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)` to ensure that deleting a ticket cascades and cleans up all related comments automatically.

### 5. How are database migrations and environments separated?
Local development targets MySQL on port `3306` with `ddl-auto=update`. Tests run against an isolated in-memory H2 database (`src/test/resources/application.properties`) with `create-drop`, ensuring that the test suite runs in under 15 seconds without polluting local databases or requiring live credentials.

