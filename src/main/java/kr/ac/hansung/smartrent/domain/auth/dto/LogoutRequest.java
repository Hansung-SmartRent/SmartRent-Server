package kr.ac.hansung.smartrent.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** pushToken: 이 휴대폰의 FCM 토큰. 있으면 지움 */
public record LogoutRequest(@NotBlank(message = "필수 값입니다.") String refreshToken, String pushToken) {
}
