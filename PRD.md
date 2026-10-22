# Product Requirements Document (PRD) — TicketDesk API

**Project:** TicketDesk API  
**Owner:** Ritik Pal  
**Version:** 1.0  
**Target delivery:** 7–10 days  
**Document status:** Implementation baseline

## 1. Product overview
TicketDesk is a REST API for managing support tickets. It supports users, tickets, comments, ticket status transitions, priority/status filtering, pagination, validation, consistent errors, and automated tests. The purpose is to build a compact, explainable Java/Spring Boot backend suitable for a Java Developer Intern portfolio.

## 2. Problem statement
Support requests need a consistent way to be created, tracked, updated, and discussed. TicketDesk provides a structured API and relational data model for those workflows.

## 3. Goals
- Implement REST CRUD operations for tickets.
- Create and retrieve users.
- Add and list comments attached to tickets.
- Enforce the ticket status workflow: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`.
- Support filtering by status and priority plus pagination.
- Validate request payloads and return a consistent JSON error shape.
- Deliver at least 15 passing automated tests.
- Provide Swagger UI, Postman examples, and setup documentation.

## 4. Non-goals for v1
- Authentication / authorization (JWT, Spring Security).
- Frontend UI.
- Docker or deployment automation.
- Notifications, file attachments, email integrations, or real-time updates.
- Production multi-tenancy or advanced audit/history features.

Do not claim non-goal features in the README or resume unless implemented later.

## 5. Users and roles
- **USER:** can be associated with tickets and comments in the domain model.
- **ADMIN:** represented as a user role in the data model.
- v1 has no authentication, so roles are data only and do not enforce permissions.

## 6. Functional requirements
### Users
- `POST /api/users`: create a user.
- `GET /api/users/{id}`: retrieve one user.
- Email must be unique.
- Duplicate email returns `409 Conflict`.
- Unknown user returns `404 Not Found`.

### Tickets
- `POST /api/tickets`: create a ticket with title, optional description, priority, and existing creator.
- New tickets start at `OPEN`.
- `GET /api/tickets`: list tickets with optional status/priority filters and pagination (`page`, `size`).
- `GET /api/tickets/{id}`: retrieve one ticket.
- `PUT /api/tickets/{id}`: update title, description, and priority.
- `PATCH /api/tickets/{id}/status`: advance status by exactly one workflow step.
- `DELETE /api/tickets/{id}`: delete a ticket; successful response is `204 No Content`.
- Title is required and must be 5–200 characters.
- Priority must be `LOW`, `MEDIUM`, or `HIGH`.
- Unknown ticket returns `404 Not Found`.
- Skipping or reversing a status step returns `400 Bad Request`.

### Comments
- `POST /api/tickets/{id}/comments`: add a comment to a ticket.
- `GET /api/tickets/{id}/comments`: list comments for a ticket.
- Comment message is required; ticket and author must exist.
- Deleting a ticket deletes its comments via the database relationship.

## 7. API quality requirements
- Use request/response DTOs; do not expose JPA entities directly.
- Controllers must delegate to services; controllers must not call repositories directly.
- Use constructor injection.
- Use Bean Validation for incoming payloads.
- Use a global exception handler and stable error response structure.
- Use HTTP status codes consistently.
- Add OpenAPI/Swagger documentation.

## 8. Acceptance criteria
- [ ] App starts locally and connects to MySQL.
- [ ] User, ticket, and comment entities and relationships work.
- [ ] All endpoints in the API specification are implemented.
- [ ] Invalid payloads return HTTP 400 with useful validation messages.
- [ ] Missing resources return HTTP 404.
- [ ] Duplicate email returns HTTP 409.
- [ ] Only the next status step is accepted.
- [ ] Ticket filtering and pagination work.
- [ ] `mvn test` passes at least 15 tests.
- [ ] README explains setup, endpoints, examples, Swagger, and test execution.
- [ ] Secrets are kept out of source control.

## 9. Success metrics
- 15+ passing automated tests.
- Approximately 70% service-layer coverage as a target; report only the measured result.
- A new developer can run the project by following the README.
- The developer can explain the controller/service/repository separation and status rule.

## 10. Assumptions and decisions
- Java 17, Spring Boot 3.x, Maven, MySQL 8.
- H2 is used for repository/integration tests where appropriate.
- The API is local-development / portfolio scope, not production-secured.
- The source guide is authoritative for v1 scope; optional enhancements belong to v2.
