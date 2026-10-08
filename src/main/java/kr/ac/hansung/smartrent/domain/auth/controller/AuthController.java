package kr.ac.hansung.smartrent.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.auth.dto.AuthTokensResponse;
import kr.ac.hansung.smartrent.domain.auth.dto.LoginRequest;
import kr.ac.hansung.smartrent.domain.auth.dto.LogoutRequest;
import kr.ac.hansung.smartrent.domain.auth.dto.RefreshRequest;
import kr.ac.hansung.smartrent.domain.auth.service.AuthService;
import kr.ac.hansung.smartrent.domain.auth.service.TokenService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final TokenService tokenService;

	@Operation(summary = "로그인")
	@PostMapping("/login")
	public ApiResponse<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@Operation(summary = "토큰 새로 받기")
	@PostMapping("/token/refresh")
	public ApiResponse<AuthTokensResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.ok(tokenService.refresh(request.refreshToken()));
	}

	@Operation(summary = "로그아웃")
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(CurrentUser.id(), request);
		return ApiResponse.ok(null);
	}
}
