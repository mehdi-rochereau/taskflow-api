> 🇬🇧 [Read in English](SECURITY.md)

# Politique de Sécurité

## Vue d'ensemble

Ce document décrit les mesures de sécurité implémentées dans l'API TaskFlow
et présente les limitations connues et les améliorations prévues.

TaskFlow est un projet portfolio démontrant les bonnes pratiques de sécurité
des API REST avec Spring Boot 3.5, authentification JWT, cookies HttpOnly et MySQL.

---

## Versions supportées

| Version | Supportée |
|---------|-----------|
| 1.0.x   | ✅        |

---

## Mesures de sécurité

### Authentification & Gestion des sessions

- **JWT Access Tokens** — Signés avec HMAC-SHA512. Valides 15 minutes.
  Stockés dans un cookie `HttpOnly` nommé `jwt` (path `/api`).
- **Rotation des Refresh Tokens** — Les refresh tokens sont à usage unique, stockés
  en MySQL, et émis dans un cookie `HttpOnly` nommé `refreshToken`
  (path `/api/auth`, expiration 7 jours). Chaque utilisation révoque le token
  précédent et en émet un nouveau.
- **Déconnexion sécurisée** — `POST /api/auth/logout` révoque tous les refresh tokens
  actifs côté serveur et efface les deux cookies HttpOnly.
- **Encodage BCrypt** — Tous les mots de passe sont hachés avec BCrypt avant
  persistance. Les mots de passe en clair ne sont jamais stockés ni loggés.
- **Mode d'authentification dual** — JWT accepté via le header
  `Authorization: Bearer` (Swagger/Postman) ou cookie `HttpOnly` (Angular).
  Le header est prioritaire.

### Nettoyage des tokens

- **Purge planifiée** — Les refresh tokens expirés et révoqués sont automatiquement
  supprimés chaque jour à 2h00 via `@Scheduled(cron = "0 0 2 * * *")`,
  évitant une croissance illimitée de la base de données.

### Sécurité du transport

- **HTTPS en production** — `COOKIE_SECURE=true` garantit que les cookies ne sont
  transmis que via HTTPS.
- **HSTS** — `Strict-Transport-Security: max-age=31536000; includeSubDomains`
  appliqué sur toutes les réponses.

### En-têtes de sécurité HTTP

| En-tête | Valeur |
|---------|--------|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | `default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'` |
| `Referrer-Policy` | `no-referrer` |

### Limitation de débit (Rate Limiting)

La limitation de débit est appliquée via Bucket4j (algorithme Token Bucket) par adresse IP :

| Endpoint | Limite |
|----------|--------|
| `POST /api/auth/login` | 5 requêtes / minute |
| `POST /api/auth/register` | 3 requêtes / heure |
| `POST /api/auth/refresh` | 20 requêtes / minute |

Les requêtes bloquées reçoivent une réponse `429 Too Many Requests` et sont
enregistrées dans le journal d'audit.

### Sanitization des entrées

Tous les champs texte fournis par l'utilisateur sont sanitizés avec
[OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer)
avant persistance pour prévenir les attaques XSS :

| Champ | Sanitizé |
|-------|----------|
| `User.username` | ✅ |
| `Project.name` | ✅ |
| `Project.description` | ✅ |
| `Task.title` | ✅ |
| `Task.description` | ✅ |

Champs exclus : `password` (BCrypt), `email` (validation `@Email`).

### Contrôle d'accès

- **Vérifications de propriété** — Chaque mutation de projet et de tâche vérifie
  que l'utilisateur authentifié est bien le propriétaire de la ressource.
- **`@PreAuthorize("isAuthenticated()")`** — Toutes les méthodes de service
  requièrent une authentification au niveau méthode via Spring Security.
- **Validation JWT** — Chaque requête protégée valide la signature JWT,
  l'expiration et l'existence de l'utilisateur avant d'accorder l'accès.

### Journal d'audit

Tous les événements de sécurité pertinents sont loggés via un logger `AUDIT` dédié :

| Événement | Niveau |
|-----------|--------|
| `LOGIN_SUCCESS` | INFO |
| `LOGIN_FAILURE` | WARN |
| `REGISTER_SUCCESS` | INFO |
| `PROJECT_DELETE` | INFO |
| `TASK_DELETE` | INFO |
| `SANITIZATION` | WARN |
| `TOKEN_PURGE` | INFO |
| `UNEXPECTED_ERROR` | ERROR |

### Gestion des erreurs

- Les stack traces et détails internes ne sont **jamais** exposés dans les réponses API.
- Toutes les exceptions non gérées retournent une réponse générique `500`
  via `GlobalExceptionHandler`.
- Les réponses `404` ne révèlent jamais si une ressource existe ou appartient
  à un autre utilisateur.

### Prévention CSRF

- **Cookies SameSite=Strict** — Tous les cookies HttpOnly utilisent `SameSite=Strict`.
- **API sans état** — Pas de cookies de session — élimine le vecteur d'attaque CSRF principal.
- **CSRF désactivé** dans Spring Security — inutile pour les API REST sans état.

---

## Principes de sécurité appliqués

| Principe | Implémentation |
|----------|----------------|
| **Défense en profondeur** | Cookies HttpOnly + sanitization + vérifications de propriété + Spring Security |
| **Moindre privilège** | Cookies scopés (`/api`, `/api/auth`), propriété vérifiée par ressource |
| **Sécurité par défaut** | Tokens invalides/expirés → `401`, accès refusé → `403`, erreurs → `500` |
| **Séparation des responsabilités** | Auth dans `UserService`, cycle de vie des tokens dans `RefreshTokenService`, audit dans `AuditService` |
| **Pas de sécurité par obscurcissement** | Sécurité basée sur des standards éprouvés (JWT, BCrypt, HttpOnly, SameSite) |

---

## Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `JWT_SECRET` | Clé de signature HMAC-SHA512 — minimum 32 caractères | — |
| `JWT_EXPIRATION` | Expiration JWT en millisecondes | `900000` (15 min) |
| `COOKIE_SECURE` | Active le flag `Secure` sur les cookies (mettre `true` en production) | `false` |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Validité du refresh token en jours | `7` |
| `DB_USERNAME` | Nom d'utilisateur MySQL | `root` |
| `DB_PASSWORD` | Mot de passe MySQL | `root` |

---

## Limitations connues

### `unsafe-inline` sur les styles

`style-src 'unsafe-inline'` est requis par Swagger UI qui injecte des styles inline
dynamiquement. Cela autorise l'injection CSS mais pas l'exécution JavaScript —
risque modéré. Une CSP basée sur des nonces éliminerait ce risque mais est
significativement plus complexe à implémenter avec springdoc-openapi.

### Limitation de débit en mémoire

Les buckets de limitation de débit sont stockés en `ConcurrentHashMap` — ils se
réinitialisent au redémarrage de l'application. Dans un déploiement multi-instances,
un cache distribué (Redis) serait nécessaire pour une limitation cohérente.

### Stockage des Refresh Tokens

Les refresh tokens sont stockés en MySQL — persistants entre les redémarrages mais
nécessitent la disponibilité de la base de données. Une approche Redis offrirait
une recherche plus rapide et une expiration automatique par TTL.

---

## Améliorations prévues

- [ ] OAuth2 Google + GitHub
- [ ] `DELETE /api/users/me` — suppression de compte pour la conformité RGPD
- [ ] `GET /api/auth/me` — endpoint de validation de session côté serveur
- [ ] Limitation de débit basée sur Redis pour les déploiements multi-instances
- [ ] CSP basée sur des nonces pour éliminer `unsafe-inline`

---

## Signaler une vulnérabilité

Si vous découvrez une vulnérabilité de sécurité dans ce projet, merci de la
signaler de manière responsable en contactant :

**Email :** mehdi.rochereau.dev@gmail.com

Merci d'inclure :
- Une description de la vulnérabilité
- Les étapes pour la reproduire
- L'impact potentiel

Ce projet est un portfolio et n'est pas destiné à un usage en production avec
de vraies données utilisateurs. Le délai de réponse peut varier.

---

## Liens

- [taskflow-ui](https://github.com/mehdi-rochereau/taskflow-ui) — Frontend Angular
- [taskflow-ui/SECURITY.md](https://github.com/mehdi-rochereau/taskflow-ui/blob/main/SECURITY.fr.md) — Politique de sécurité frontend