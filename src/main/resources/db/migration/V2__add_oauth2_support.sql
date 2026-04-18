-- Add provider column to users table
ALTER TABLE users
    ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- Create user_providers table for multi-provider support
CREATE TABLE user_providers (
                                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id       BIGINT       NOT NULL,
                                provider      VARCHAR(20)  NOT NULL,
                                provider_id   VARCHAR(255) NOT NULL,
                                provider_email VARCHAR(255),
                                linked_at     DATETIME(6)  NOT NULL,
                                CONSTRAINT fk_user_providers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                CONSTRAINT uq_provider_id UNIQUE (provider, provider_id),
                                CONSTRAINT uq_user_provider UNIQUE (user_id, provider)
);