CREATE TABLE retained_warnings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 student_number VARCHAR(20) NOT NULL,
 kind VARCHAR(10) NOT NULL,
 created_at DATETIME NOT NULL,
 revoked_at DATETIME NULL,
 expires_at DATETIME NULL,
 original_suspended_until DATETIME NULL,
 retain_until DATETIME NOT NULL,
 KEY idx_retained_number (student_number),
 KEY idx_retained_expiry (retain_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
