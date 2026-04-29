# TaskFlow API

> 🇫🇷 [Version française](README.fr.md)

🌐 **Live:** [taskflow.mehdi-rochereau.dev](https://taskflow.mehdi-rochereau.dev)
📖 **API Docs:** [api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html](https://api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html)

A RESTful task management API built with Java 21 and Spring Boot 3.5, featuring stateless JWT authentication, HttpOnly cookie-based session management, ownership-based access control, input sanitization and comprehensive test coverage.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Coverage](https://img.shields.io/badge/Coverage-93%25-brightgreen?style=flat)]()

---

## Overview

TaskFlow API allows authenticated users to manage projects and tasks. Each user owns their projects and controls access to the associated tasks. The API follows REST conventions and returns structured JSON responses for all operations, including errors.

For security details, see [SECURITY.md](SECURITY.md).

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8.4 |
| Migrations | Flyway |
| Build tool | Gradle |
| Testing | JUnit 5 + Mockito + MockMvc |
| Coverage | JaCoCo |
| Documentation | Springdoc OpenAPI / Swagger UI |
| Rate Limiting | Bucket4j |
| Input Sanitization | OWASP Java HTML Sanitizer |

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
└── user/               # User entity, repository, service, DTOs
```

The application follows a standard layered architecture:
- **Controllers** handle HTTP routing and input validation
- **Services** encapsulate business logic and ownership enforcement
- **Repositories** provide data access via Spring Data JPA
- **DTOs** decouple the API contract from internal entities

---

## Features

- Stateless JWT authentication — register and login with username or email
- HttpOnly cookie-based session management — JWT (15 min) + Refresh Token (7 days)
- Refresh token rotation — single-use tokens, automatic renewal
- Full CRUD on projects and tasks
- Ownership-based access control — users can only access their own resources
- Task filtering by status and priority
- Input sanitization via OWASP Java HTML Sanitizer — XSS prevention
- Rate limiting on authentication endpoints — brute force protection
- Audit logging for all security-relevant events
- Scheduled cleanup of expired and revoked refresh tokens
- Centralized error handling with structured JSON responses
- i18n error messages — English and French
- Database schema versioning with Flyway
- Interactive API documentation via Swagger UI
- 159 tests — 93%+ coverage (JUnit 5 + Mockito + MockMvc)

---

## Prerequisites

- Java 21
- MySQL 8.4 — either installed locally or via Docker

---

## Getting Started

**1. Configure environment variables**

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | HMAC-SHA512 signing key — min 32 chars | — |
| `JWT_EXPIRATION` | JWT expiry in milliseconds | `900000` (15 min) |
| `COOKIE_SECURE` | Enable `Secure` flag on cookies | `false` |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Refresh token validity in days | `7` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `root` |

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

| Method | Endpoint | Description | Auth required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Create a new account | No |
| POST | `/api/auth/login` | Log in — sets JWT + refresh token cookies | No |
| POST | `/api/auth/refresh` | Refresh JWT using refresh token cookie | No |
| POST | `/api/auth/logout` | Log out — revokes refresh token, clears cookies | No |

### Projects

| Method | Endpoint | Description | Auth required |
|--------|----------|-------------|---------------|
| GET | `/api/projects` | List my projects | Yes |
| GET | `/api/projects/{id}` | Get a project by ID | Yes |
| POST | `/api/projects` | Create a project | Yes |
| PUT | `/api/projects/{id}` | Update a project | Yes |
| DELETE | `/api/projects/{id}` | Delete a project | Yes |

### Tasks

| Method | Endpoint | Description | Auth required |
|--------|----------|-------------|---------------|
| GET | `/api/projects/{projectId}/tasks` | List tasks (filters: `?status=TODO`, `?priority=HIGH`) | Yes |
| GET | `/api/projects/{projectId}/tasks/{id}` | Get a task by ID | Yes |
| POST | `/api/projects/{projectId}/tasks` | Create a task | Yes |
| PUT | `/api/projects/{projectId}/tasks/{id}` | Update a task | Yes |
| DELETE | `/api/projects/{projectId}/tasks/{id}` | Delete a task | Yes |

Protected endpoints accept authentication via:
- `Authorization: Bearer <token>` header — for Swagger UI and Postman
- `jwt` HttpOnly cookie — for Angular (sent automatically by the browser)

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

The JaCoCo coverage report is generated at:

```
build/reports/jacoco/html/index.html
```

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
    "title": ["Title is required"]
  }
}
```

---

## Planned Improvements

- [ ] GitHub Actions CI/CD
- [ ] OAuth2 login (Google / GitHub)

---

## Ecosystem

| Repository | Description |
|------------|-------------|
| [taskflow-api](https://github.com/mehdi-rochereau/taskflow-api) | Spring Boot REST API (this repo) |
| [taskflow-ui](https://github.com/mehdi-rochereau/taskflow-ui) | Angular frontend |
| [taskflow-deploy](https://github.com/mehdi-rochereau/taskflow-deploy) | Docker Compose, Nginx, deployment scripts |
| [SECURITY.md](SECURITY.md) | API security policy |
| [taskflow-ui/SECURITY.md](https://github.com/mehdi-rochereau/taskflow-ui/blob/main/SECURITY.md) | Frontend security policy |
```
