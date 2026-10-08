package kr.ac.hansung.smartrent.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "필수 값입니다.") String refreshToken) {
}
