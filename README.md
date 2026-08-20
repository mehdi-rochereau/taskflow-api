# TaskFlow API

> 🇫🇷 [Version française](README.fr.md)

🌐 **Live:** [taskflow.mehdi-rochereau.dev](https://taskflow.mehdi-rochereau.dev)

📖 **API
Docs:** [api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html](https://api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html)

A RESTful task management API built with Java 21 and Spring Boot 3.5, featuring stateless JWT authentication, HttpOnly
cookie-based session management, ownership-based access control, input sanitization and comprehensive test coverage.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![CI/CD](https://github.com/mehdi-rochereau/taskflow-api/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/mehdi-rochereau/taskflow-api/actions/workflows/ci-cd.yml)
[![codecov](https://codecov.io/gh/mehdi-rochereau/taskflow-api/graph/badge.svg)](https://codecov.io/gh/mehdi-rochereau/taskflow-api)

---

## Overview

TaskFlow API allows authenticated users to manage projects and tasks. Each user owns their projects and controls access
to the associated tasks. The API follows REST conventions and returns structured JSON responses for all operations,
including errors.

For security details, see [SECURITY.md](SECURITY.md).

---

## Tech Stack

| Layer              | Technology                                   |
|--------------------|----------------------------------------------|
| Language           | Java 21                                      |
| Framework          | Spring Boot 3.5                              |
| Security           | Spring Security + JWT (jjwt 0.12.6)          |
| Persistence        | Spring Data JPA / Hibernate                  |
| Database           | MySQL 8.4                                    |
| Migrations         | Flyway                                       |
| Build tool         | Gradle                                       |
| Testing            | JUnit 5 + Mockito + MockMvc + Testcontainers |
| Coverage           | JaCoCo                                       |
| Documentation      | Springdoc OpenAPI / Swagger UI               |
| Rate Limiting      | Bucket4j                                     |
| Input Sanitization | OWASP Java HTML Sanitizer                    |
| CI/CD              | GitHub Actions + Docker + Trivy              |
| Container          | Docker + ghcr.io                             |
| Deployment         | Hetzner VPS + Nginx + Let's Encrypt          |

---

## Architecture

```
src/main/java/com/mehdi/taskflow/
├── auth/               # Refresh token entity, repository, service, cleanup job
│   └── AuthController  # Authentication endpoints
├── config/             # Security, OpenAPI, Cookie, Sanitization, Audit configuration
├── exception/          # Global exception handler
├── project/            # Project entity, repository, service, controller, DTOs
├── security/           # JWT filter, JWT service, Rate limit filter, UserDetailsService
├── task/               # Task entity, repository, service, controller, DTOs
└── user/               # User entity, repository, service, controller, DTOs
```

The application follows a standard layered architecture:

- **Controllers** handle HTTP routing and input validation
- **Services** encapsulate business logic and ownership enforcement
- **Repositories** provide data access via Spring Data JPA
- **DTOs** decouple the API contract from internal entities

OpenAPI annotations live in dedicated `*ControllerApi` interfaces, which the controllers implement. Documentation
concerns stay out of the routing code.

---

## Features

- Stateless JWT authentication — register and login with username or email
- HttpOnly cookie-based session management — JWT (15 min) + Refresh Token (7 days)
- Refresh token rotation — single-use tokens, automatic renewal
- Full CRUD on projects and tasks
- Account management — profile update, password change, account deletion
- Ownership-based access control — users can only access their own resources
- Task filtering by status and priority
- Input sanitization via OWASP Java HTML Sanitizer — XSS prevention
- Rate limiting on authentication endpoints — brute force protection
- Audit logging for all security-relevant events
- Scheduled cleanup of expired and revoked refresh tokens
- Centralized error handling with structured JSON responses
- i18n error messages — English and French
- Database schema versioning with Flyway
- Referential integrity enforced at database level — cascade rules and CHECK constraints, not application-side
  conventions
- Interactive API documentation via Swagger UI
- 80%+ coverage (JUnit 5 + Mockito + MockMvc + Testcontainers)

---

## Prerequisites

- Java 21
- MySQL 8.4 — either installed locally or via Docker
- Docker — required to run the integration test suite

---

## Getting Started

**1. Configure environment variables**

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

| Variable                        | Description                                                              | Default                 |
|---------------------------------|--------------------------------------------------------------------------|-------------------------|
| `JWT_SECRET`                    | HMAC-SHA512 signing key — min 32 chars                                   | —                       |
| `JWT_EXPIRATION`                | JWT expiry in milliseconds                                               | `900000` (15 min)       |
| `COOKIE_SECURE`                 | Enable `Secure` flag on cookies                                          | `false`                 |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Refresh token validity in days                                           | `7`                     |
| `CORS_ALLOWED_ORIGINS`          | Comma-separated list of origins allowed to call the API with credentials | `http://localhost:4200` |
| `DB_USERNAME`                   | MySQL username                                                           | `root`                  |
| `DB_PASSWORD`                   | MySQL password                                                           | `root`                  |

**2. Start MySQL**

Option A — MySQL already installed locally:

```sql
CREATE DATABASE taskflow;
```

Option B — MySQL via Docker:

```bash
docker run --name taskflow-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=taskflow \
  -p 3306:3306 \
  -d mysql:8.4
```

**3. Run the application**

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8082`.

**4. Access Swagger UI**

```
http://localhost:8082/swagger-ui/index.html
```

---

## API Reference

### Authentication

| Method | Endpoint             | Description                                     | Auth required |
|--------|----------------------|-------------------------------------------------|---------------|
| POST   | `/api/auth/register` | Create a new account                            | No            |
| POST   | `/api/auth/login`    | Log in — sets JWT + refresh token cookies       | No            |
| POST   | `/api/auth/refresh`  | Refresh JWT using refresh token cookie          | No            |
| POST   | `/api/auth/logout`   | Log out — revokes refresh token, clears cookies | No            |

### User

| Method | Endpoint                 | Description                               | Auth required |
|--------|--------------------------|-------------------------------------------|---------------|
| GET    | `/api/users/me`          | Get my profile                            | Yes           |
| PUT    | `/api/users/me`          | Update my username and email              | Yes           |
| POST   | `/api/users/me/password` | Change my password — revokes all sessions | Yes           |
| DELETE | `/api/users/me`          | Delete my account permanently             | Yes           |

Account deletion requires password confirmation and is irreversible. It removes the account, the projects it owns, the
tasks inside those projects, the refresh tokens and the linked authentication providers. Tasks merely assigned to the
deleted user inside someone else's project are preserved and become unassigned:
a task belongs to its project, not to its assignee.

### Projects

| Method | Endpoint             | Description         | Auth required |
|--------|----------------------|---------------------|---------------|
| GET    | `/api/projects`      | List my projects    | Yes           |
| GET    | `/api/projects/{id}` | Get a project by ID | Yes           |
| POST   | `/api/projects`      | Create a project    | Yes           |
| PUT    | `/api/projects/{id}` | Update a project    | Yes           |
| DELETE | `/api/projects/{id}` | Delete a project    | Yes           |

### Tasks

| Method | Endpoint                               | Description                                            | Auth required |
|--------|----------------------------------------|--------------------------------------------------------|---------------|
| GET    | `/api/projects/{projectId}/tasks`      | List tasks (filters: `?status=TODO`, `?priority=HIGH`) | Yes           |
| GET    | `/api/projects/{projectId}/tasks/{id}` | Get a task by ID                                       | Yes           |
| POST   | `/api/projects/{projectId}/tasks`      | Create a task                                          | Yes           |
| PUT    | `/api/projects/{projectId}/tasks/{id}` | Update a task                                          | Yes           |
| DELETE | `/api/projects/{projectId}/tasks/{id}` | Delete a task                                          | Yes           |

Protected endpoints authenticate through the `jwt` HttpOnly cookie, and through nothing else. The browser sends it
automatically, and so do Postman and the Swagger UI, both of which keep a cookie jar: `Try it out` works with no
`Authorize` step. The `Authorization: Bearer` header was accepted until August 2026 and no longer is.

---

## Authentication Flow

```
POST /api/auth/login
→ JWT cookie (15 min) + Refresh Token cookie (7 days)

JWT expired → POST /api/auth/refresh
→ New JWT cookie + New Refresh Token cookie (rotation)

POST /api/auth/logout
→ Refresh token revoked server-side + cookies cleared
```

For full security details including rate limiting, cookie configuration and audit logging,
see [SECURITY.md](SECURITY.md).

---

## Running Tests

```bash
./gradlew test
```

The suite is organised in three layers:

| Layer       | Annotation                      | What it proves                                    |
|-------------|---------------------------------|---------------------------------------------------|
| Unit        | none — Mockito only             | Business logic of each service in isolation       |
| Web slice   | `@WebMvcTest`                   | Routing, validation, status codes, security rules |
| Integration | `@DataJpaTest` + Testcontainers | Behaviour owned by the database engine            |

Integration tests (`*IT`) start a disposable MySQL 8.4 container matching the production engine, and let Flyway replay
every migration against it. They cover what no mock can prove: foreign key `ON DELETE` rules, `CHECK` constraints, and
the agreement between the schema and the JPA entities under `ddl-auto=validate`.

**Docker must be running** for these tests to execute.

### Code formatting

Formatting is applied by Spotless, using the AOSP variant of `google-java-format`. Checkstyle reports style violations
but never corrects them; Spotless is the tool that does the correcting, and the two are configured to agree.

```bash
./gradlew spotlessCheck    # reports files that are not formatted
./gradlew spotlessApply    # rewrites them
```

A versioned `pre-push` hook runs `spotlessCheck` and refuses a push whose sources are not formatted. Git does not track
`.git/hooks`, so the hook lives in
`.githooks` and each clone activates it once:

```bash
git config core.hooksPath .githooks
```

Without that command the hook is present in the repository and inert, which is worse than having none: the safeguard
looks in place and is not. To bypass it for a single push, use `git push --no-verify`.

Formatting on save is the more comfortable path, but it is an editor setting and therefore neither versioned nor
uniform. The hook and the build are what decide, whatever the editor.

<details>
<summary>Setting up formatting on save in IntelliJ IDEA</summary>

IntelliJ's built-in Java formatter is not `google-java-format`, so without the plugin below it would reformat on save
into a shape Spotless undoes on the next run. The two tools have to agree.

1. `Settings > Plugins > Marketplace`, install **google-java-format**, restart.

2. `Help > Edit Custom VM Options`, append the export list published by the plugin. It opens internal compiler packages
   that the Java module system has kept closed since Java 9. Gradle passes the same flags on its own, which is why the
   command line never needs this. Take the list from the plugin's own notification: a partial list leaves the plugin
   unable to start.

3. Restart, then `Settings > Tools > google-java-format`: tick **Enable**, and select **AOSP**, not the default Google
   style. Both wrap at 100 columns, but Google indents with 2 spaces and this codebase uses 4. Picking the wrong one
   reindents every file you save.

4. `Settings > Tools > Actions on Save`, tick **Reformat code**. Leave **Optimize imports** off: the star-import
   thresholds are already tuned under
   `Settings > Editor > Code Style > Java > Imports`, and Spotless removes unused imports on its side.

Verify by breaking the indentation of a line, saving, and watching it snap back. Then run `./gradlew spotlessCheck`: if
the build fails on a file IntelliJ just formatted, the variant is wrong.

</details>

VS Code formats Java through the Eclipse engine bundled with the Red Hat extension, a different formatter with a
different result. Matching it to the build means maintaining an Eclipse formatter file alongside the Spotless
configuration, a second place describing the same rules. Running `./gradlew spotlessApply` before pushing is simpler and
is what the hook checks anyway.

### Test naming conventions

Test methods are named `shouldExpectedBehaviour_whenPrecondition`. The underscore separates what is asserted from the
condition under which it holds, so a failing build names the broken rule without anyone opening the file. Fixture values
are written as literals rather than extracted into named constants, which keeps the value next to the assertion that
reads it.

Both conventions break a rule that applies to production sources, `MethodName`
and `MagicNumber` respectively. They are exempted for `src/test` only, in
`config/checkstyle/suppressions.xml`, where each exemption carries its reason. That file is unrelated to
`config/owasp/suppressions.xml`, which silences CVE findings for Dependency-Check.

To run only the fast tests:

```bash
./gradlew test --tests "*Test"
```

The JaCoCo coverage report is generated at:

```
build/reports/jacoco/html/index.html
```

---

## CI/CD Pipeline

Every push triggers an automated pipeline:

| Step                | Tool                               | Details                                                                                  |
|---------------------|------------------------------------|------------------------------------------------------------------------------------------|
| Secret scanning     | GitLeaks                           | Full history scan                                                                        |
| Formatting          | Spotless                           | google-java-format, AOSP variant; blocks on any deviation                                |
| Code style          | Checkstyle                         | Google Style variant; blocks on any violation                                            |
| Tests               | JUnit 5 + Mockito + Testcontainers | Unit, web slice and integration layers                                                   |
| Coverage            | JaCoCo + Codecov                   | 80% threshold                                                                            |
| Dependency CVEs     | OWASP Dependency Check             | NVD database, clean report; remaining findings carry dated suppressions                  |
| Docker image scan   | Trivy                              | Blocks on CRITICAL CVEs                                                                  |
| Deployment          | SSH + shared deployment script     | Hetzner VPS, single script shared with the frontend pipeline, aborts on registry refusal |
| Deploy verification | Image digest                       | Running container compared to published image                                            |
| Health check        | Spring Actuator                    | 3 min retry                                                                              |
| Rollback            | Automatic                          | On health check failure                                                                  |

Push to `main` → CI passes → Docker image built → deployed to production automatically.

---

## Database Migrations

Schema changes are versioned with Flyway and applied automatically at startup.
`spring.jpa.hibernate.ddl-auto` is set to `validate`: Hibernate never alters the schema, it only checks that the
entities match it, and refuses to start if they diverge.

| Version | Contents                                                                      |
|---------|-------------------------------------------------------------------------------|
| V1      | Initial schema — users, projects, tasks                                       |
| V2      | OAuth2 groundwork — `user_providers` table                                    |
| V3      | Cascade deletion of tasks with their project                                  |
| V4      | Refresh token table                                                           |
| V5      | Referential integrity fixes, enum `CHECK` constraints, microsecond timestamps |

---

## Error Responses

All errors follow a consistent JSON structure:

```json
{
  "timestamp": "2026-04-08T10:00:00",
  "status": 404,
  "message": "Project not found"
}
```

Validation errors include field-level details:

```json
{
  "timestamp": "2026-04-08T10:00:00",
  "status": 400,
  "errors": {
     "title": [
        "Title is required"
     ]
  }
}
```

---

## Planned Improvements

- [ ] Password reset via email
- [ ] Project sharing between users
- [ ] Third-party sign-in (Google / GitHub)
- [ ] Angular frontend integration for account management
- [ ] Prometheus + Grafana monitoring
- [ ] Redis-based rate limiting

---

## Project Management & Documentation

The three TaskFlow repositories are managed from a single
[GitHub Project](https://github.com/users/mehdi-rochereau/projects/4):
issue first, branch created from the issue, pull request, squash merge, with a five-status workflow (Backlog → In
Progress → In Review → Verifying → Done).

Cross-repository documentation lives in
[taskflow-deploy/docs](https://github.com/mehdi-rochereau/taskflow-deploy/tree/main/docs), including the full project
management manual.

---

## Ecosystem

| Repository                                                                                      | Description                               |
|-------------------------------------------------------------------------------------------------|-------------------------------------------|
| [taskflow-api](https://github.com/mehdi-rochereau/taskflow-api)                                 | Spring Boot REST API (this repo)          |
| [taskflow-ui](https://github.com/mehdi-rochereau/taskflow-ui)                                   | Angular frontend                          |
| [taskflow-deploy](https://github.com/mehdi-rochereau/taskflow-deploy)                           | Docker Compose, Nginx, deployment scripts |
| [SECURITY.md](SECURITY.md)                                                                      | API security policy                       |
| [taskflow-ui/SECURITY.md](https://github.com/mehdi-rochereau/taskflow-ui/blob/main/SECURITY.md) | Frontend security policy                  |
