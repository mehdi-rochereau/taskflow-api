CREATE TABLE refresh_tokens (
                                id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
                                token      VARCHAR(255) NOT NULL UNIQUE,
                                user_id    BIGINT       NOT NULL,
                                expires_at DATETIME     NOT NULL,
                                revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at DATETIME     NOT NULL,
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);