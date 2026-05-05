> 🇫🇷 [Lire en français](SECURITY.fr.md)

# Security Policy

## Overview

This document describes the security measures implemented in the TaskFlow API
and outlines known limitations and planned improvements.

TaskFlow is a portfolio project demonstrating modern REST API security practices
with Spring Boot 3.5, JWT authentication, HttpOnly cookies and MySQL.

---

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅        |

---

## Security Measures

### Authentication & Session Management

- **JWT Access Tokens** — Signed with HMAC-SHA512. Valid for 15 minutes.
  Stored in an `HttpOnly` cookie named `jwt` (path `/api`).
- **Refresh Token Rotation** — Refresh tokens are single-use, stored in MySQL,
  and issued as an `HttpOnly` cookie named `refreshToken` (path `/api/auth`, 7-day expiry).
  Each use revokes the previous token and issues a new one.
- **Secure Logout** — `POST /api/auth/logout` revokes all active refresh tokens
  server-side and clears both HttpOnly cookies.
- **BCrypt Password Encoding** — All passwords are hashed with BCrypt before persistence.
  Plain-text passwords are never stored or logged.
- **Dual Authentication Mode** — JWT accepted via `Authorization: Bearer` header
  (Swagger/Postman) or `HttpOnly` cookie (Angular). Header takes precedence.

### Token Cleanup

- **Scheduled Purge** — Expired and revoked refresh tokens are automatically deleted
  daily at 2:00 AM via `@Scheduled(cron = "0 0 2 * * *")`, preventing unbounded
  database growth.

### Transport Security

- **HTTPS in production** — `COOKIE_SECURE=true` ensures cookies are only
  transmitted over HTTPS.
- **HSTS** — `Strict-Transport-Security: max-age=31536000; includeSubDomains`
  enforced on all responses.

### HTTP Security Headers

| Header | Value |
|--------|-------|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | `default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'` |
| `Referrer-Policy` | `no-referrer` |

### Rate Limiting

Rate limiting is enforced via Bucket4j (Token Bucket algorithm) per IP address:

| Endpoint | Limit |
|----------|-------|
| `POST /api/auth/login` | 5 requests / minute |
| `POST /api/auth/register` | 3 requests / hour |
| `POST /api/auth/refresh` | 20 requests / minute |

Blocked requests receive a `429 Too Many Requests` response and are logged
in the audit log.

### Input Sanitization

All user-provided text fields are sanitized using the
[OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer)
before persistence to prevent XSS attacks:

| Field | Sanitized |
|-------|-----------|
| `User.username` | ✅ |
| `Project.name` | ✅ |
| `Project.description` | ✅ |
| `Task.title` | ✅ |
| `Task.description` | ✅ |

Fields excluded from sanitization: `password` (BCrypt), `email` (`@Email` validation).

### Access Control

- **Ownership Checks** — Every project and task mutation verifies that the
  authenticated user is the resource owner before any database operation.
- **`@PreAuthorize("isAuthenticated()")`** — All service methods require
  authentication at the method level via Spring Security.
- **JWT Validation** — Every protected request validates the JWT signature,
  expiration and user existence before granting access.

### Audit Logging

All security-relevant events are logged via a dedicated `AUDIT` logger:

| Event | Level |
|-------|-------|
| `LOGIN_SUCCESS` | INFO |
| `LOGIN_FAILURE` | WARN |
| `REGISTER_SUCCESS` | INFO |
| `PROJECT_DELETE` | INFO |
| `TASK_DELETE` | INFO |
| `SANITIZATION` | WARN |
| `TOKEN_PURGE` | INFO |
| `UNEXPECTED_ERROR` | ERROR |

### CI/CD Security

The deployment pipeline integrates multiple security controls:

| Control | Tool | Details |
|---------|------|---------|
| Secret scanning | GitLeaks | Full git history scanned on every push |
| Dependency CVEs | OWASP Dependency Check | NVD database, blocks on CVSS ≥ 9 |
| Docker image scan | Trivy | Blocks deployment on CRITICAL CVEs |
| Least privilege | GITHUB_TOKEN | No PAT — scoped token with minimal permissions |
| Dedicated SSH key | Ed25519 | GitHub Actions-only key, separate from developer keys |
| Branch protection | GitHub Rulesets | CI must pass before any merge to main |
| Immutable deploys | Image digest | Trivy scans the exact pushed digest, not a mutable tag |
| Automatic rollback | Docker | Previous image restored if health check fails post-deploy |

### Error Handling

- Stack traces and internal details are **never** exposed in API responses.
- All unhandled exceptions return a generic `500` response via `GlobalExceptionHandler`.
- `404` responses never reveal whether a resource exists or belongs to another user.

### CSRF Prevention

- **SameSite=Strict cookies** — All HttpOnly cookies use `SameSite=Strict`.
- **Stateless API** — No session cookies — eliminates the primary CSRF attack vector.
- **CSRF disabled** in Spring Security — not needed for stateless REST APIs.

---

## Security Principles Applied

| Principle | Implementation |
|-----------|----------------|
| **Defense in Depth** | HttpOnly cookies + input sanitization + ownership checks + Spring Security |
| **Least Privilege** | Scoped cookies (`/api`, `/api/auth`), ownership enforced per resource |
| **Fail Secure** | Invalid/expired tokens → `401`, access denied → `403`, unexpected errors → `500` |
| **Separation of Concerns** | Auth logic in `UserService`, token lifecycle in `RefreshTokenService`, audit in `AuditService` |
| **No Security by Obscurity** | Security relies on proven standards (JWT, BCrypt, HttpOnly, SameSite) |

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | HMAC-SHA512 signing key — must be at least 32 characters | — |
| `JWT_EXPIRATION` | JWT expiry in milliseconds | `900000` (15 min) |
| `COOKIE_SECURE` | Enable `Secure` flag on cookies (set `true` in production) | `false` |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Refresh token validity in days | `7` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `root` |

---

## Known Limitations

### `unsafe-inline` on Styles

`style-src 'unsafe-inline'` is required for Swagger UI which injects inline styles
dynamically. This allows CSS injection but not JavaScript execution — risk is moderate.
A nonce-based CSP would eliminate this but is significantly more complex to implement
with springdoc-openapi.

### In-Memory Rate Limiting

Rate limiting buckets are stored in `ConcurrentHashMap` — they reset on application
restart. In a multi-instance deployment, a distributed cache (Redis) would be required
for consistent rate limiting across instances.

### Refresh Token Storage

Refresh tokens are stored in MySQL — persistent across restarts but requires
database availability. A Redis-based approach would offer faster lookup and
automatic TTL-based expiry.

---

## Planned Improvements

- [ ] OAuth2 Google + GitHub
- [ ] `DELETE /api/users/me` — account deletion for GDPR compliance
- [ ] `GET /api/auth/me` — server-side session validation endpoint
- [ ] Redis-based rate limiting for multi-instance deployments
- [ ] Trivy scan on HIGH severity (currently CRITICAL only)
- [ ] Nonce-based CSP to eliminate `unsafe-inline`

---

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it
responsibly by contacting:

**Email:** mehdi.rochereau.dev@gmail.com

Please include:
- A description of the vulnerability
- Steps to reproduce
- Potential impact

This is a portfolio project and is not intended for production use with real user data.
Response time may vary.

---

## Related

- [taskflow-ui](https://github.com/mehdi-rochereau/taskflow-ui) — Angular frontend
- [taskflow-ui/SECURITY.md](https://github.com/mehdi-rochereau/taskflow-ui/blob/main/SECURITY.md) — Frontend security policy