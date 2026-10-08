package kr.ac.hansung.smartrent.domain.auth.service;

import java.util.Locale;

import kr.ac.hansung.smartrent.domain.auth.dto.AuthTokensResponse;
import kr.ac.hansung.smartrent.domain.auth.dto.LoginRequest;
import kr.ac.hansung.smartrent.domain.auth.dto.LogoutRequest;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.PushTokenRemover;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·로그아웃(B1-04). 승인 전·반려·정지 학생도 로그인은 됩니다(API 명세 1-5절).
 * 없는 이메일과 틀린 비밀번호는 같은 INVALID_CREDENTIALS로 답하고, 둘 다 실패 횟수에 셉니다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final LoginFailureService loginFailureService;
	private final TokenService tokenService;
	private final PushTokenRemover pushTokenRemover;

	public AuthTokensResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (loginFailureService.isLocked(email)) {
			throw new BusinessException(ErrorCode.LOGIN_LOCKED);
		}
		User user = userRepository.findByEmail(email)
			.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
			.orElse(null);
		if (user == null) {
			loginFailureService.recordFailure(email);
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		loginFailureService.clear(email);
		return tokenService.issue(user);
	}

	@Transactional
	public void logout(Long userId, LogoutRequest request) {
		tokenService.revoke(userId, request.refreshToken());
		if (request.pushToken() != null && !request.pushToken().isBlank()) {
			pushTokenRemover.remove(userId, request.pushToken());
		}
	}
}
