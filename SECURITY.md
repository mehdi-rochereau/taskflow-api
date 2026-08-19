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
- **Cookie-Only Transport** — The JWT is read from the `jwt` HttpOnly cookie and
  from no other source. The `Authorization: Bearer` header was accepted until
  August 2026 as a leftover from client-side token storage; it was removed once
  it was established that Postman and the Swagger UI both authenticate through
  the cookie jar alone. No response body ever carries a token value.

### Account Management

- **Password Change** — `POST /api/users/me/password` verifies the current
  password against the stored hash, rejects a new password identical to the old
  one, and revokes every active refresh token. All sessions are invalidated on
  all devices.
- **Password Confirmation on Deletion** — `DELETE /api/users/me` requires the
  account password in the request body. A stolen session alone cannot destroy
  an account.
- **Right to Erasure** — Account deletion is irreversible and removes every
  piece of data the account owns: the user record, owned projects, the tasks
  inside those projects, refresh tokens and linked authentication providers.
  Enforced by `ON DELETE CASCADE` on the foreign keys, not by application code.
- **Third-Party Data Preserved** — Tasks assigned to the deleted user inside a
  project owned by someone else survive and become unassigned, via
  `ON DELETE SET NULL`. Deleting an account must not destroy another user's work.

### Token Cleanup

- **Scheduled Purge** — Expired and revoked refresh tokens are automatically deleted
  daily at 2:00 AM via `@Scheduled(cron = "0 0 2 * * *")`, preventing unbounded
  database growth.

### Transport Security

- **HTTPS in production** — `COOKIE_SECURE=true` ensures cookies are only
  transmitted over HTTPS.
- **HSTS** — `Strict-Transport-Security` is set by the Nginx reverse proxy, not
  by this application. Spring Security only emits the header on a request it
  considers secure, and the request reaches the application in plain HTTP from
  the proxy, which terminates TLS. Placing the header at the proxy also covers
  the frontend host, which is not a Spring application. See
  [taskflow-deploy](https://github.com/mehdi-rochereau/taskflow-deploy) for the
  vhost configuration.

### HTTP Security Headers

| Header | Value |
|--------|-------|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `X-XSS-Protection` | `0` |
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

### Data Integrity

Integrity rules are enforced by the database schema rather than by application
conventions. A rule held by the engine applies whatever the access path — the
API, a maintenance script, or a direct SQL session — while a rule held in Java
only applies when the code is the one doing the writing.

| Constraint | Rule | Purpose |
|------------|------|---------|
| `fk_projects_owner` | `ON DELETE CASCADE` | Owned projects follow their owner |
| `fk_tasks_project` | `ON DELETE CASCADE` | Tasks follow their project |
| `fk_tasks_assignee` | `ON DELETE SET NULL` | Assigned tasks survive their assignee |
| `fk_refresh_tokens_user` | `ON DELETE CASCADE` | No token outlives its account |
| `fk_user_providers_user` | `ON DELETE CASCADE` | No provider link outlives its account |
| `chk_tasks_status` | `CHECK` | Only valid `TaskStatus` values persist |
| `chk_tasks_priority` | `CHECK` | Only valid `TaskPriority` values persist |

Schema changes are versioned with Flyway and applied at startup.
`spring.jpa.hibernate.ddl-auto` is set to `validate`: Hibernate never alters the
schema and refuses to start if the entities and the schema diverge. Integration
tests replay every migration against a disposable MySQL container matching the
production engine, so these rules are verified on every push rather than
assumed.

### Audit Logging

All security-relevant events are logged via a dedicated `AUDIT` logger:

| Event | Level |
|-------|-------|
| `LOGIN_SUCCESS` | INFO |
| `LOGIN_FAILURE` | WARN |
| `REGISTER_SUCCESS` | INFO |
| `PASSWORD_CHANGE` | INFO |
| `PROFILE_UPDATE` | INFO |
| `ACCOUNT_DELETE` | WARN |
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
| Dependency CVEs | OWASP Dependency Check | NVD database, `failBuildOnCVSS = 9`; the step is non-blocking for now, so the threshold reports without stopping the pipeline |
| Docker image scan | Trivy | Blocks deployment on CRITICAL CVEs |
| Least privilege | GITHUB_TOKEN | No PAT — scoped token with minimal permissions |
| Dedicated SSH key | Ed25519 | GitHub Actions-only key, separate from developer keys |
| Branch protection | GitHub Rulesets | CI must pass before any merge to main |
| Immutable deploys | Image digest | Trivy scans the exact pushed digest, not a mutable tag |
| Deploy verification | Image digest | The running container is compared to the digest published by the run, and the job fails on mismatch |
| Fail-loud deployment | Strict-mode deployment script | A registry refusal aborts the deploy step instead of falling through to a success message |
| Single deployment path | Shared script | Deployment and rollback live in one reviewed script in `taskflow-deploy`, called by both application pipelines, instead of inline blocks duplicated per repository |
| Automatic rollback | Docker | Previous image restored if health check fails post-deploy |
| Scheduled database backup | systemd timer | Daily logical dump with completeness checks and seven-day rotation |

The registry token installed on the production host is scoped to
`read:packages` only. The host pulls images and never pushes or deletes them,
so a leak of that credential cannot be used to publish a tampered image.

### Error Handling

- Stack traces and internal details are **never** exposed in API responses.
- All unhandled exceptions return a generic `500` response via `GlobalExceptionHandler`.
- `404` responses never reveal whether a resource exists or belongs to another user.

### CSRF Prevention

- **SameSite=Strict cookies** — All HttpOnly cookies use `SameSite=Strict`, so
  the browser never attaches them to a cross-site request. This is what removes
  the CSRF vector, not an absence of cookies: authentication does travel in
  cookies here.
- **Restrictive CORS policy** — Allowed origins are declared explicitly and
  credentials are enabled, which forbids a wildcard. An unlisted origin is
  rejected before reaching any handler. Two origins are declared in production:
  the frontend, and the API's own origin. The latter is needed because the
  Swagger UI is served from the API host and the browser still sends an `Origin`
  header, which since Spring Framework 5.3 makes the request a CORS request even
  when origin and target share the same host.
- **CSRF disabled** in Spring Security — a deliberate decision resting on the
  two controls above.

---

## Security Principles Applied

| Principle | Implementation |
|-----------|----------------|
| **Defense in Depth** | HttpOnly cookies + input sanitization + ownership checks + Spring Security + database-level constraints |
| **Least Privilege** | Scoped cookies (`/api`, `/api/auth`), ownership enforced per resource, registry token limited to `read:packages` |
| **Fail Secure** | Invalid/expired tokens → `401`, access denied → `403`, unexpected errors → `500`, schema mismatch → application refuses to start, unverified deployment → pipeline fails |
| **Separation of Concerns** | Auth logic in `UserService`, token lifecycle in `RefreshTokenService`, audit in `AuditService`, integrity in the schema |
| **No Security by Obscurity** | Security relies on proven standards (JWT, BCrypt, HttpOnly, SameSite) |

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | HMAC-SHA512 signing key — must be at least 32 characters | — |
| `JWT_EXPIRATION` | JWT expiry in milliseconds | `900000` (15 min) |
| `COOKIE_SECURE` | Enable `Secure` flag on cookies (set `true` in production) | `false` |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Refresh token validity in days | `7` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed to call the API with credentials. Production holds two: the frontend and the API's own origin, the latter required by the Swagger UI | `http://localhost:4200` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `root` |

The defaults above are development values. In production the database account is
a dedicated user with a strong password, not `root`.

`CORS_ALLOWED_ORIGINS` is split on commas and trimmed before being handed to
Spring Security. A wildcard is not an option here: credentials are enabled so
that the browser attaches the authentication cookies, and the CORS specification
forbids combining a wildcard origin with credentials. Origins are therefore
enumerated, and an unlisted one is rejected before reaching any handler.

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

### No Account Recovery

There is no password reset flow. A user who forgets their password loses access
to their account and its data permanently. This is the most serious gap in the
product and it requires email delivery to be addressed.

### Unexercised Rollback Path

The rollback step now records an immutable image reference and can restore the
previous version, but it has never been triggered by a real failure. Exercising
it requires a deliberate health check failure and therefore a controlled outage,
which is now unblocked: automated backups have been in place since 15 August
2026. Until it is exercised, the recovery path is implemented and reviewed, not
proven.

### Backups Are Local to the Host

A daily logical dump runs on the production host with seven-day rotation, so the
recovery point objective is at most twenty-four hours. The dumps live on the same
machine as the database: a host loss takes both. Hetzner snapshots cover that
case at the machine level, but there is no off-site copy of the database itself.

### Bundled Swagger UI Vulnerabilities

Springdoc 2.8.8 bundles swagger-ui 5.21.0, which in turn bundles DOMPurify
3.2.4: eighteen CVEs plus `GHSA-55q2-fjhq-7xh7`, highest MEDIUM 6.3. This is the
only remaining entry in the Dependency-Check report. The component is genuinely
shipped, so no honest suppression is available, and remediation depends on
whether a fixed Springdoc release exists that is still compatible with Spring
Boot 3.5.

### Spring Boot 3.5 End of Open Source Life

The 3.5 line reached open source end of life on 30 June 2026, and 3.5.16 is the
last release published to Maven Central for it. Any future CVE affecting Spring
Framework 6.2, Spring Security 6.5 or Spring Boot 3.5 itself will have no free
upstream fix. Third-party components remain patchable through explicit version
overrides, which is how Tomcat and Jackson are currently kept current. Migrating
to 4.x is therefore a deadline rather than an option.

---

## Planned Improvements

- [ ] Password reset via email
- [ ] Exercise the rollback path against a controlled failure
- [ ] Off-site copy of the database dumps
- [ ] `GET /api/auth/me` — server-side session validation endpoint
- [ ] Redis-based rate limiting for multi-instance deployments
- [ ] Trivy scan on HIGH severity (currently CRITICAL only)
- [ ] Nonce-based CSP to eliminate `unsafe-inline`
- [ ] Third-party sign-in (Google / GitHub)
- [ ] Migrate to Spring Boot 4.x before the 3.5 line becomes a liability
- [ ] Remove `continue-on-error` from the OWASP step once the report is clean
