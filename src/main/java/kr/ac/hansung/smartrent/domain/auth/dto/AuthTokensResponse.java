package kr.ac.hansung.smartrent.domain.auth.dto;

import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;

/** openapi.yaml AuthTokens. accessTokenExpiresIn은 초 단위(1800) */
public record AuthTokensResponse(String accessToken, String refreshToken, long accessTokenExpiresIn, MeResponse user) {
}
