package kr.ac.hansung.smartrent.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.auth.EmailPolicy;
import kr.ac.hansung.smartrent.domain.auth.entity.VerificationPurpose;
import kr.ac.hansung.smartrent.domain.auth.repository.RefreshTokenRepository;
import kr.ac.hansung.smartrent.domain.auth.service.EmailVerificationService;
import kr.ac.hansung.smartrent.domain.user.dto.EmailChangeRequest;
import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;
import kr.ac.hansung.smartrent.domain.user.dto.PasswordChangeRequest;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.ActiveWarningCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 정보·비밀번호 변경·이메일 변경(B1-06) */
@Service
@RequiredArgsConstructor
public class MyInfoService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailVerificationService verificationService;
	private final PasswordEncoder passwordEncoder;
	private final ActiveWarningCounter warningCounter;
	private final Clock clock;

	@Transactional(readOnly = true)
	public MeResponse me(Long userId) {
		return toMe(load(userId));
	}

	/** 현재 이메일로 받은 PASSWORD_RESET 번호로 확인 후 변경. 모든 기기의 로그인 유지(리프레시 토큰)를 끊음 */
	@Transactional
	public void changePassword(Long userId, PasswordChangeRequest request) {
		User user = load(userId);
		verificationService.consume(user.getEmail(), VerificationPurpose.PASSWORD_RESET, request.code());
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.deleteAllByUserId(userId);
	}

	/** 새 학교 이메일로 받은 EMAIL_CHANGE 번호로 확인 후 변경. 다른 사람이 쓰는 주소면 409 */
	@Transactional
	public MeResponse changeEmail(Long userId, EmailChangeRequest request) {
		User user = load(userId);
		String email = EmailPolicy.requireSchoolEmail(request.newEmail());
		userRepository.findByEmail(email).filter(other -> !other.getId().equals(userId)).ifPresent(other -> {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
		});
		verificationService.consume(email, VerificationPurpose.EMAIL_CHANGE, request.code());
		user.changeEmail(email);
		return toMe(user);
	}

	private User load(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	/** 유효 경고 수는 B2-12의 ActiveWarningCounter(그 전까지 0) */
	private MeResponse toMe(User user) {
		return MeResponse.of(user, warningCounter.count(user.getId()), LocalDateTime.now(clock));
	}
}
