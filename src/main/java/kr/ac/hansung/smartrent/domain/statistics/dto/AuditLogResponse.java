package kr.ac.hansung.smartrent.domain.statistics.dto;

import java.time.LocalDateTime;

/** openapi AuditLog. 처리한 관리자가 없으면(서버 자동 처리, 지운 계정) actorName은 null */
public record AuditLogResponse(Long id, String action, String detail, String actorName, LocalDateTime createdAt) {
}
