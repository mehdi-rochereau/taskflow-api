# TaskFlow API

> 🇬🇧 [English version](README.md)

🌐 **En ligne :** [taskflow.mehdi-rochereau.dev](https://taskflow.mehdi-rochereau.dev)

📖 **Documentation API :** [api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html](https://api.taskflow.mehdi-rochereau.dev/swagger-ui/index.html)

Une API REST de gestion de tâches développée en Java 21 et Spring Boot 3.5, avec authentification JWT stateless, gestion de session par cookies HttpOnly, contrôle d'accès par propriétaire, sanitization des entrées et une couverture de tests complète.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![CI/CD](https://github.com/mehdi-rochereau/taskflow-api/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/mehdi-rochereau/taskflow-api/actions/workflows/ci-cd.yml)
[![codecov](https://codecov.io/gh/mehdi-rochereau/taskflow-api/graph/badge.svg)](https://codecov.io/gh/mehdi-rochereau/taskflow-api)

---

## Présentation

TaskFlow API permet à des utilisateurs authentifiés de gérer des projets et des tâches. Chaque utilisateur est propriétaire de ses projets et contrôle l'accès aux tâches associées. L'API suit les conventions REST et retourne des réponses JSON structurées pour toutes les opérations, y compris les erreurs.

Pour les détails de sécurité, voir [SECURITY.fr.md](SECURITY.fr.md).

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Langage | Java 21 |
| Framework | Spring Boot 3.5 |
| Sécurité | Spring Security + JWT (jjwt 0.12.6) |
| Persistance | Spring Data JPA / Hibernate |
| Base de données | MySQL 8.4 |
| Migrations | Flyway |
| Build | Gradle |
| Tests | JUnit 5 + Mockito + MockMvc |
| Couverture | JaCoCo |
| Documentation | Springdoc OpenAPI / Swagger UI |
| Limitation de débit | Bucket4j |
| Sanitization | OWASP Java HTML Sanitizer |
| CI/CD | GitHub Actions + Docker + Trivy |
| Conteneur | Docker + ghcr.io |
| Déploiement | Hetzner VPS + Nginx + Let's Encrypt |
---

## Architecture

```
src/main/java/com/mehdi/taskflow/
├── auth/               # Entité, repository, service refresh token, job de nettoyage
│   └── AuthController  # Endpoints d'authentification
├── config/             # Configuration Spring Security, OpenAPI, Cookie, Sanitization, Audit
├── exception/          # Gestionnaire d'exceptions centralisé
├── project/            # Entité, repository, service, controller, DTOs
├── security/           # Filtre JWT, service JWT, filtre rate limiting, UserDetailsService
├── task/               # Entité, repository, service, controller, DTOs
└── user/               # Entité, repository, service, DTOs
```

L'application suit une architecture en couches standard :
- **Controllers** — routing HTTP et validation des entrées
- **Services** — logique métier et contrôle des droits d'accès
- **Repositories** — accès aux données via Spring Data JPA
- **DTOs** — découplage entre le contrat API et les entités internes

---

## Fonctionnalités

- Authentification JWT stateless — inscription et connexion par username ou email
- Gestion de session par cookies HttpOnly — JWT (15 min) + Refresh Token (7 jours)
- Rotation des refresh tokens — tokens à usage unique, renouvellement automatique
- CRUD complet sur les projets et les tâches
- Contrôle d'accès par propriétaire — chaque utilisateur n'accède qu'à ses propres ressources
- Filtrage des tâches par statut et priorité
- Sanitization des entrées via OWASP Java HTML Sanitizer — prévention XSS
- Limitation de débit sur les endpoints d'authentification — protection brute force
- Journal d'audit pour tous les événements de sécurité
- Purge planifiée des refresh tokens expirés et révoqués
- Gestion centralisée des erreurs avec réponses JSON structurées
- Messages d'erreur i18n — anglais et français
- Versioning du schéma de base de données avec Flyway
- Documentation API interactive via Swagger UI
- Couverture > 80% (JUnit 5 + Mockito + MockMvc)

---

## Prérequis

- Java 21
- MySQL 8.4 — installé en local ou via Docker

---

## Lancer le projet en local

**1. Configurer les variables d'environnement**

Copie `.env.example` vers `.env` et renseigne tes valeurs :

```bash
cp .env.example .env
```

| Variable | Description | Défaut |
|----------|-------------|--------|
| `JWT_SECRET` | Clé de signature HMAC-SHA512 — min 32 caractères | — |
| `JWT_EXPIRATION` | Expiration JWT en millisecondes | `900000` (15 min) |
| `COOKIE_SECURE` | Active le flag `Secure` sur les cookies | `false` |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Validité du refresh token en jours | `7` |
| `DB_USERNAME` | Nom d'utilisateur MySQL | `root` |
| `DB_PASSWORD` | Mot de passe MySQL | `root` |

**2. Démarrer MySQL**

Option A — MySQL déjà installé en local :

```sql
CREATE DATABASE taskflow;
```

Option B — MySQL via Docker :

```bash
docker run --name taskflow-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=taskflow \
  -p 3306:3306 \
  -d mysql:8.4
```

**3. Lancer l'application**

```bash
./gradlew bootRun
```

L'application démarre sur `http://localhost:8082`.

**4. Accéder à Swagger UI**

```
http://localhost:8082/swagger-ui/index.html
```

---

## Référence API

### Authentification

| Méthode | Endpoint | Description | Auth requise |
|---------|----------|-------------|--------------|
| POST | `/api/auth/register` | Créer un compte | Non |
| POST | `/api/auth/login` | Se connecter — définit les cookies JWT + refresh token | Non |
| POST | `/api/auth/refresh` | Rafraîchir le JWT via le cookie refresh token | Non |
| POST | `/api/auth/logout` | Se déconnecter — révoque le refresh token, efface les cookies | Non |

### Projets

| Méthode | Endpoint | Description | Auth requise |
|---------|----------|-------------|--------------|
| GET | `/api/projects` | Lister mes projets | Oui |
| GET | `/api/projects/{id}` | Récupérer un projet | Oui |
| POST | `/api/projects` | Créer un projet | Oui |
| PUT | `/api/projects/{id}` | Modifier un projet | Oui |
| DELETE | `/api/projects/{id}` | Supprimer un projet | Oui |

### Tâches

| Méthode | Endpoint | Description | Auth requise |
|---------|----------|-------------|--------------|
| GET | `/api/projects/{projectId}/tasks` | Lister les tâches (filtres : `?status=TODO`, `?priority=HIGH`) | Oui |
| GET | `/api/projects/{projectId}/tasks/{id}` | Récupérer une tâche | Oui |
| POST | `/api/projects/{projectId}/tasks` | Créer une tâche | Oui |
| PUT | `/api/projects/{projectId}/tasks/{id}` | Modifier une tâche | Oui |
| DELETE | `/api/projects/{projectId}/tasks/{id}` | Supprimer une tâche | Oui |

Les endpoints protégés acceptent l'authentification via :
- Header `Authorization: Bearer <token>` — pour Swagger UI et Postman
- Cookie HttpOnly `jwt` — pour Angular (envoyé automatiquement par le navigateur)

---

## Flux d'authentification

```
POST /api/auth/login
→ Cookie JWT (15 min) + Cookie Refresh Token (7 jours)

JWT expiré → POST /api/auth/refresh
→ Nouveau cookie JWT + Nouveau cookie Refresh Token (rotation)

POST /api/auth/logout
→ Refresh token révoqué côté serveur + cookies effacés
```

Pour les détails complets sur la sécurité — limitation de débit, configuration des cookies et journal d'audit — voir [SECURITY.fr.md](SECURITY.fr.md).

---

## Lancer les tests

```bash
./gradlew test
```

Le rapport de couverture JaCoCo est généré dans :

```
build/reports/jacoco/html/index.html
```

---

## Pipeline CI/CD

Chaque push déclenche un pipeline automatisé :

| Étape | Outil | Détails |
|-------|-------|---------|
| Scan de secrets | GitLeaks | Historique complet |
| Style de code | Checkstyle | Google Style |
| Tests | JUnit 5 + Mockito | 159 tests |
| Couverture | JaCoCo + Codecov | Seuil 80% |
| CVE dépendances | OWASP Dependency Check | Base NVD |
| Scan image Docker | Trivy | Bloque sur CVE CRITICAL |
| Déploiement | SSH + Docker Compose | VPS Hetzner |
| Health check | Spring Actuator | 3 min de retry |
| Rollback | Automatique | En cas d'échec health check |

Push sur `main` → CI vert → image Docker buildée → déployée en production automatiquement.

---

## Format des erreurs

Toutes les erreurs suivent une structure JSON cohérente :

```json
{
  "timestamp": "2026-04-08T10:00:00",
  "status": 404,
  "message": "Projet introuvable"
}
```

Les erreurs de validation incluent le détail par champ :

```json
{
  "timestamp": "2026-04-08T10:00:00",
  "status": 400,
  "errors": {
    "title": ["Le titre est obligatoire"]
  }
}
```

---

## Améliorations prévues

- [ ] Connexion OAuth2 (Google / GitHub)
- [ ] Intégration frontend Angular
- [ ] Monitoring Prometheus + Grafana
- [ ] Rate limiting Redis

---

## Écosystème

| Dépôt                                                                                                 | Description |
|-------------------------------------------------------------------------------------------------------|-------------|
| [taskflow-api](https://github.com/mehdi-rochereau/taskflow-api)                                       | API REST Spring Boot (ce dépôt) |
| [taskflow-ui](https://github.com/mehdi-rochereau/taskflow-ui)                                         | Frontend Angular |
| [taskflow-deploy](https://github.com/mehdi-rochereau/taskflow-deploy) | Docker Compose, Nginx, scripts de déploiement |
| [SECURITY.fr.md](SECURITY.fr.md)                                                                      | Politique de sécurité API |
| [taskflow-ui/SECURITY.fr.md](https://github.com/mehdi-rochereau/taskflow-ui/blob/main/SECURITY.fr.md) | Politique de sécurité frontend |
```
