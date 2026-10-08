-- 백엔드 1 담당 표(B1-02). 칸 이름·자료형·NOT NULL·유일 조건은 docs/db/README.md 3절과 같습니다.
-- 새 표나 칸은 이 파일을 고치지 말고 다음 번호 파일(V2__...sql)을 새로 만듭니다(Flyway).

CREATE TABLE users (
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    role                         VARCHAR(10)  NOT NULL,
    email                        VARCHAR(100) NOT NULL,
    password_hash                VARCHAR(100) NOT NULL,
    name                         VARCHAR(30)  NULL,
    student_number               VARCHAR(20)  NULL,
    approval                     VARCHAR(10)  NOT NULL,
    reject_reason                VARCHAR(200) NULL,
    student_id_image_key         VARCHAR(200) NULL,
    profile_image_key            VARCHAR(200) NULL,
    suspended_until              DATETIME     NULL,
    suspended_until_return       BOOLEAN      NOT NULL DEFAULT FALSE,
    suspension_started_by_return BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                   DATETIME     NOT NULL,
    updated_at                   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_student_number (student_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE email_verifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(100) NOT NULL,
    purpose    VARCHAR(20)  NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at DATETIME     NOT NULL,
    used_at    DATETIME     NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_email_verifications_email_purpose (email, purpose)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE login_failures (
    email        VARCHAR(100) NOT NULL,
    fail_count   INT          NOT NULL,
    locked_until DATETIME     NULL,
    PRIMARY KEY (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(100) NOT NULL,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE equipment_models (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    source             VARCHAR(10)  NOT NULL,
    example_for        VARCHAR(10)  NULL,
    model_key          VARCHAR(20)  NOT NULL,
    site_number        VARCHAR(10)  NULL,
    category           VARCHAR(20)  NOT NULL,
    name               VARCHAR(100) NOT NULL,
    name_key           VARCHAR(100) NULL,
    manufacturer       VARCHAR(50)  NULL,
    purchase_year      VARCHAR(4)   NULL,
    purpose            VARCHAR(200) NULL,
    rental_type        VARCHAR(10)  NOT NULL,
    multi_day          BOOLEAN      NOT NULL DEFAULT FALSE,
    max_rental_days    INT          NULL,
    max_rental_minutes INT          NULL,
    carry_out          BOOLEAN      NOT NULL DEFAULT FALSE,
    location           VARCHAR(100) NOT NULL,
    contact            VARCHAR(30)  NULL,
    open_time          TIME         NOT NULL,
    close_time         TIME         NOT NULL,
    interval_minutes   INT          NOT NULL,
    guide              TEXT         NULL,
    archived           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_models_model_key (model_key),
    -- 운영 중인 모델만 채우고 운영 종료하면 NULL. NULL은 여러 개여도 됨
    UNIQUE KEY uk_equipment_models_name_key (name_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE model_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    model_id   BIGINT       NOT NULL,
    image_key  VARCHAR(200) NOT NULL,
    sort_order INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_images_model_order (model_id, sort_order),
    CONSTRAINT fk_model_images_model FOREIGN KEY (model_id) REFERENCES equipment_models (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE equipment_units (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    model_id    BIGINT       NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    `condition` VARCHAR(12)  NOT NULL,
    deleted_at  DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_units_code (code),
    KEY idx_equipment_units_model (model_id),
    CONSTRAINT fk_equipment_units_model FOREIGN KEY (model_id) REFERENCES equipment_models (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE operating_hours (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    model_id   BIGINT NOT NULL,
    date       DATE   NULL,
    open_time  TIME   NOT NULL,
    close_time TIME   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_operating_hours_model_date (model_id, date),
    CONSTRAINT fk_operating_hours_model FOREIGN KEY (model_id) REFERENCES equipment_models (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE holidays (
    date          DATE        NOT NULL,
    name          VARCHAR(50) NOT NULL,
    source        VARCHAR(10) NOT NULL,
    open_override BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at    DATETIME    NOT NULL,
    PRIMARY KEY (date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notices (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(100) NOT NULL,
    body       TEXT         NOT NULL,
    author_id  BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notices_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 탈퇴하면 그 학생의 문의·구매 요청도 지움(DB 설계 4절)
CREATE TABLE inquiries (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(100) NOT NULL,
    body         TEXT         NOT NULL,
    response     TEXT         NULL,
    responded_at DATETIME     NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_inquiries_user (user_id),
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE purchase_requests (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(100) NOT NULL,
    body         TEXT         NOT NULL,
    response     TEXT         NULL,
    responded_at DATETIME     NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_purchase_requests_user (user_id),
    CONSTRAINT fk_purchase_requests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 탈퇴해도 비용·품질 확인용 기록은 남기고 누가 요청했는지만 지움
CREATE TABLE recommendation_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NULL,
    purpose       VARCHAR(200) NOT NULL,
    result_json   TEXT         NOT NULL,
    model_id_used VARCHAR(100) NULL,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recommendation_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 관리자 처리 기록. 판정 작업 같은 서버 자동 처리는 actor_id가 비어 있음
CREATE TABLE audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    action       VARCHAR(50)  NOT NULL,
    detail       VARCHAR(500) NOT NULL,
    actor_id     BIGINT       NULL,
    retain_until DATETIME     NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_logs_created (created_at),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
