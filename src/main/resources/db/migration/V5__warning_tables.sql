CREATE TABLE warnings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 kind VARCHAR(10) NOT NULL,
 rental_id BIGINT NULL,
 created_at DATETIME NOT NULL,
 revoked_at DATETIME NULL,
 revoke_reason VARCHAR(100) NULL,
 expires_at DATETIME NULL,
 UNIQUE KEY uk_warning_rental_kind (rental_id, kind),
 KEY idx_warnings_user_active (user_id, revoked_at, expires_at),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
