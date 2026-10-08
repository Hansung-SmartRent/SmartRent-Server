-- B1-08 파일 삭제 재시도 (DB 설계 3절 storage_deletion_jobs). 계정과 묶지 않음(외래 키 없음)
CREATE TABLE storage_deletion_jobs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    storage_mode  VARCHAR(10)  NOT NULL,
    object_key    VARCHAR(512) NOT NULL,
    created_at    DATETIME     NOT NULL,
    attempted_at  DATETIME     NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
