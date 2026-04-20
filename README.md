# TaskFlow API

> 🇫🇷 [Version française](README.fr.md)

A RESTful task management API built with Java 21 and Spring Boot 3.5, featuring stateless JWT authentication, ownership-based access control, and comprehensive test coverage.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Coverage](https://img.shields.io/badge/Coverage-93%25-brightgreen?style=flat)]()

---

## Overview

TaskFlow API allows authenticated users to manage projects and tasks. Each user owns their projects and controls access to the associated tasks. The API follows REST conventions and returns structured JSON responses for all operations, including errors.

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

---

## Architecture

```
src/main/java/com/mehdi/taskflow/
├── auth/               # Authentication controller
├── config/             # Security and OpenAPI configuration
├── exception/          # Global exception handler
├── project/            # Project entity, repository, service, controller, DTOs
├── security/           # JWT filter, JWT service, UserDetailsService
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
- Full CRUD on projects and tasks
- Ownership-based access control — users can only access their own resources
- Task filtering by status and priority
- Centralized error handling with structured JSON responses
- Database schema versioning with Flyway
- Interactive API documentation via Swagger UI
- 93%+ test coverage across 98 tests (unit + integration)

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

Or use the default values already set in `application.yml` — suitable for local development.

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
| POST | `/api/auth/login` | Log in — returns a JWT token | No |

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
| GET | `/api/projects/{projectId}/tasks` | List tasks (optional filters: `?status=TODO`, `?priority=HIGH`) | Yes |
| GET | `/api/projects/{projectId}/tasks/{id}` | Get a task by ID | Yes |
| POST | `/api/projects/{projectId}/tasks` | Create a task | Yes |
| PUT | `/api/projects/{projectId}/tasks/{id}` | Update a task | Yes |
| DELETE | `/api/projects/{projectId}/tasks/{id}` | Delete a task | Yes |

All protected endpoints require an `Authorization: Bearer <token>` header.

---

## Authentication Flow

```
POST /api/auth/login
→ Returns a JWT token (valid 24h)
→ Include in subsequent requests: Authorization: Bearer <token>
```

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
    "title": "Le titre est obligatoire"
  }
}
```

---

## Planned Improvements

- Angular frontend
- Docker Compose + GitHub Actions CI/CD
- Deployment on a Hetzner VPS (Docker + Nginx + custom domain)
- OAuth2 login (Google / GitHub)
- Two-factor authentication (TOTP)
- Internationalization of error messages (EN/FR)
- Pagination on list endpoints
