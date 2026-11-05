CREATE TABLE notifications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 kind VARCHAR(20) NOT NULL,
 title VARCHAR(100) NOT NULL,
 body VARCHAR(1000) NOT NULL,
 related_type VARCHAR(20) NULL,
 related_id BIGINT NULL,
 is_read BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME NOT NULL,
 KEY idx_notifications_user_created (user_id, created_at, id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE notification_settings (
 user_id BIGINT NOT NULL PRIMARY KEY,
 due_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE push_tokens (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 token VARCHAR(512) NOT NULL,
 platform VARCHAR(10) NOT NULL,
 updated_at DATETIME NOT NULL,
 UNIQUE KEY uk_push_token (token),
 KEY idx_push_tokens_user (user_id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
