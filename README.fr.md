# TaskFlow API

> 🇬🇧 [English version](README.md)

Une API REST de gestion de tâches développée en Java 21 et Spring Boot 3.5, avec authentification JWT stateless, contrôle d'accès par propriétaire et une couverture de tests complète.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Coverage](https://img.shields.io/badge/Coverage-93%25-brightgreen?style=flat)]()

---

## Présentation

TaskFlow API permet à des utilisateurs authentifiés de gérer des projets et des tâches. Chaque utilisateur est propriétaire de ses projets et contrôle l'accès aux tâches associées. L'API suit les conventions REST et retourne des réponses JSON structurées pour toutes les opérations, y compris les erreurs.

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

---

## Architecture

```
src/main/java/com/mehdi/taskflow/
├── auth/               # Controller d'authentification
├── config/             # Configuration Spring Security et OpenAPI
├── exception/          # Gestionnaire d'exceptions centralisé
├── project/            # Entité, repository, service, controller, DTOs
├── security/           # Filtre JWT, service JWT, UserDetailsService
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
- CRUD complet sur les projets et les tâches
- Contrôle d'accès par propriétaire — chaque utilisateur n'accède qu'à ses propres ressources
- Filtrage des tâches par statut et priorité
- Gestion centralisée des erreurs avec réponses JSON structurées
- Versioning du schéma de base de données avec Flyway
- Documentation API interactive via Swagger UI
- Couverture de tests > 93% sur 98 tests (unitaires + intégration)

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

Ou utilise les valeurs par défaut déjà définies dans `application.yml` — suffisant pour un environnement de développement local.

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
| POST | `/api/auth/login` | Se connecter — retourne un token JWT | Non |

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

Tous les endpoints protégés nécessitent un header `Authorization: Bearer <token>`.

---

## Flux d'authentification

```
POST /api/auth/login
→ Retourne un token JWT (valide 24h)
→ À inclure dans les requêtes suivantes : Authorization: Bearer <token>
```

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
    "title": "Le titre est obligatoire"
  }
}
```

---

## Améliorations prévues

- Frontend Angular
- Docker Compose + GitHub Actions CI/CD
- Déploiement sur un VPS Hetzner (Docker + Nginx + nom de domaine)
- Connexion OAuth2 (Google / GitHub)
- Authentification à deux facteurs (TOTP)
- Internationalisation des messages d'erreur (EN/FR)
- Pagination sur les endpoints de liste