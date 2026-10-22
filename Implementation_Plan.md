# Implementation Plan — TicketDesk API (7–10 Days)

## Working rules
- Build only the v1 scope first; do not add JWT, frontend, Docker, or unrelated features.
- Implement in small steps and run tests after each meaningful change.
- Keep code beginner-friendly and explainable line by line.
- Do not claim features, test counts, or coverage that have not been verified.
- Commit daily with clear messages.

## Day 1 — Environment and project bootstrap
- Install JDK 17, IntelliJ IDEA Community, MySQL 8, Postman, Git.
- Generate Maven project from Spring Initializr with Spring Web, Spring Data JPA, MySQL Driver, Validation, and H2 test dependency.
- Add compatible springdoc-openapi dependency.
- Create `ticketdesk` database and configure `DB_PASSWORD` as an environment variable.
- Start the app and verify database connectivity.
**Done when:** app starts without errors and database connection works.

## Day 2 — Entities and repositories
- Implement `User`, `Ticket`, `Comment`, `UserRole`, `TicketStatus`, and `Priority`.
- Map relationships and timestamps.
- Implement repositories.
- Write initial repository test(s).
**Done when:** entity mappings load and persistence can be tested.

## Day 3 — User and ticket creation/read
- Add DTOs and mapping.
- Implement user create/get.
- Implement ticket create/get/list.
- Keep controllers thin and put business logic in services.
**Done when:** Postman can create a user and ticket, then retrieve them.

## Day 4 — Update/delete and status workflow
- Implement ticket update/delete.
- Implement strict one-step status transition.
- Add service tests for valid and invalid transitions.
**Done when:** skipping or reversing a status returns HTTP 400.

## Day 5 — Validation and error handling
- Add Bean Validation annotations.
- Implement custom `ResourceNotFoundException`, `InvalidStatusTransitionException`, and duplicate-email handling.
- Implement `ApiError` and `GlobalExceptionHandler`.
**Done when:** all errors use the documented JSON shape and correct status code.

## Day 6 — Filtering, pagination, and comments
- Add optional status/priority filters and `page`/`size`.
- Add comment create/list endpoints.
- Validate ticket and comment author references.
- Add tests for filter combinations and pagination.
**Done when:** query parameters and comment workflow work through Postman.

## Day 7 — Service tests
- Write at least 8 service unit tests using JUnit 5 and Mockito.
- Cover missing user, missing ticket, duplicate email, and status transitions.
**Done when:** service test suite passes.

## Day 8 — Controller and repository tests
- Write at least 5 controller tests using MockMvc.
- Write at least 2 repository tests using H2.
- Cover validation and HTTP response codes.
**Done when:** 15 or more tests pass with `mvn test`.

## Day 9 — Swagger, Postman, README
- Document all endpoints and examples.
- Export a Postman collection if useful.
- Write setup instructions, database configuration, endpoint table, Swagger URL, and test command.
- Add architecture diagram/package tree.
**Done when:** a reviewer can run the project using only the README.

## Day 10 — Review and interview preparation
- Run `mvn clean test`.
- Check formatting, package names, secrets, `.gitignore`, and Git history.
- Measure coverage if available; only report actual measured numbers.
- Practise explaining OOP, dependency injection, DTOs, JPA relationships, validation, exception handling, pagination, and status transitions.
**Done when:** build is clean and every class can be explained.

## Definition of done
- [ ] All in-scope endpoints implemented.
- [ ] Request validation and consistent errors implemented.
- [ ] Status transition rules tested.
- [ ] At least 15 tests pass.
- [ ] Swagger UI and README are usable.
- [ ] No credentials committed.
- [ ] GitHub repository name: `ticketdesk-api`.
- [ ] Git topics: `java`, `spring-boot`, `rest-api`, `mysql`, `junit5`, `mockito`.

## Antigravity execution prompt
Use these six Markdown files as the project specification. First inspect the existing workspace and report its current files, Java version, Maven setup, and database configuration. Then implement the smallest complete vertical slice at a time, following the scope and sequence in `Implementation_Plan.md`. Do not silently expand scope. Before changing files, state the intended changes briefly. After each phase, run relevant tests/build commands and report actual results. Never invent successful tests or coverage. Keep secrets out of source control. If a requirement conflicts across documents, flag it and ask before making a consequential decision.
