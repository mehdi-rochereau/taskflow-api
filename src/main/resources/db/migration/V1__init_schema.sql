CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    created_at DATETIME     NOT NULL
    );

CREATE TABLE IF NOT EXISTS projects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id    BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  DATETIME     NOT NULL,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users (id)
    );

CREATE TABLE IF NOT EXISTS tasks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    assignee_id BIGINT,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    due_date    DATE,
    created_at  DATETIME     NOT NULL,
    CONSTRAINT fk_task_project  FOREIGN KEY (project_id)  REFERENCES projects (id),
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
    );