# Technical Requirements Document (TRD) — TicketDesk API

## 1. Technology stack
- Java 17 (LTS)
- Spring Boot 3.x: Spring Web, Spring Data JPA, Bean Validation
- MySQL 8 for local development
- H2 for tests
- Maven
- JUnit 5, Mockito, MockMvc
- springdoc-openapi / Swagger UI
- IntelliJ IDEA Community, Postman, Git and GitHub

Use the latest compatible stable versions supported by the generated Spring Boot 3.x project. Check the current springdoc-openapi version before adding it. If an annotation is deprecated in the selected Spring version, use the supported replacement suggested by the IDE.

## 2. Architecture
Layered architecture:

`HTTP request → Controller → Service → Repository → Database`

- **Controller:** HTTP mapping, request binding, validation entry point, response status.
- **Service:** business rules, resource checks, status transition validation, transaction boundaries as needed.
- **Repository:** Spring Data JPA persistence and query methods.
- **Entity:** JPA persistence model.
- **DTO:** API request and response contracts.
- **Exception:** domain exceptions and global error mapping.

Controllers must not access repositories directly. Return DTOs, not entities. Use constructor injection rather than field injection.

## 3. Suggested package layout
```text
com.ritik.ticketdesk
├── controller
│   ├── UserController.java
│   ├── TicketController.java
│   └── CommentController.java
├── service
│   ├── UserService.java
│   ├── TicketService.java
│   └── CommentService.java
├── repository
│   ├── UserRepository.java
│   ├── TicketRepository.java
│   └── CommentRepository.java
├── entity
│   ├── User.java
│   ├── Ticket.java
│   ├── Comment.java
│   ├── UserRole.java
│   ├── TicketStatus.java
│   └── Priority.java
├── dto
├── exception
├── config
└── TicketDeskApplication.java
```

## 4. Runtime configuration
Example `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ticketdesk
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```
- Create the database once: `CREATE DATABASE ticketdesk;`
- Set `DB_PASSWORD` in the local environment; never commit the real password.
- `ddl-auto=update` is acceptable for this learning project. For a production system, prefer reviewed schema migrations.
- Add `target/`, `.idea/`, local environment files, and secrets to `.gitignore`.

## 5. Technical constraints
- Use enums persisted as strings: `@Enumerated(EnumType.STRING)`.
- Use lazy loading for many-to-one relations where suitable.
- Use DTO mapping to avoid accidental entity serialization and lazy-loading issues.
- Use `Pageable` and `Page<Ticket>` for paginated ticket results.
- Keep status-transition logic in the domain/service layer and test it.
- Use `@Valid` on request DTOs.
- Use `@RestControllerAdvice` for consistent error responses.
- Avoid adding security, frontend, Docker, or other out-of-scope features during v1.

## 6. Build and run
Prerequisites: JDK 17, Maven (or Maven Wrapper), MySQL 8, Git.

```bash
java -version
mvn -version
mvn test
mvn spring-boot:run
```

If the project includes Maven Wrapper, prefer `./mvnw` on macOS/Linux or `mvnw.cmd` on Windows.

## 7. Test strategy
- Service unit tests: JUnit 5 + Mockito, minimum 8.
- Controller tests: `@WebMvcTest` + MockMvc, minimum 5.
- Repository tests: `@DataJpaTest` with H2, minimum 2.
- Total minimum: 15 passing tests.
- Cover happy paths, invalid input, missing resources, duplicate email, valid transitions, skipped/backwards transitions, filters and pagination.

## 8. Security and reliability baseline
- No authentication/authorization in v1; explicitly document this limitation.
- No credentials in Git.
- Validate all external input.
- Do not return stack traces or internal implementation details in API errors.
- Ensure error responses are consistent.
- Use database constraints for unique email and foreign keys.
