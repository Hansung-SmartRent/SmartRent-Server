package kr.ac.hansung.smartrent.global.security;

import java.util.List;

/** 로그인 없이 부르는 주소(API 명세 1-5절 "권한 없음" 5개 + 헬스 체크·Swagger·개발용 사진 주소) */
public final class PublicPaths {

	public static final List<String> PATHS = List.of(
		"/api/v1/auth/email-codes",
		"/api/v1/auth/signup",
		"/api/v1/auth/login",
		"/api/v1/auth/token/refresh",
		"/api/v1/auth/password/reset",
		"/api/v1/health",
		// 개발용 사진 임시 주소(STORAGE_MODE=local). 서명·만료 시각으로 확인
		"/api/v1/files/local/**",
		"/swagger-ui.html",
		"/swagger-ui/**",
		"/v3/api-docs/**",
		"/error");

	private PublicPaths() {
	}
}
