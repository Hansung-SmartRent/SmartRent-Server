package kr.ac.hansung.smartrent.domain.auth.dto;

import java.time.LocalDateTime;

/** openapi.yaml EmailCodeSent: 인증번호 만료 시각(보낸 뒤 5분) */
public record EmailCodeSentResponse(LocalDateTime expiresAt) {
}
