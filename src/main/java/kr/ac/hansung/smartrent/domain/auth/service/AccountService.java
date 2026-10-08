package kr.ac.hansung.smartrent.domain.auth.service;

import kr.ac.hansung.smartrent.domain.auth.EmailPolicy;
import kr.ac.hansung.smartrent.domain.auth.dto.AuthTokensResponse;
import kr.ac.hansung.smartrent.domain.auth.dto.EmailCodeRequest;
import kr.ac.hansung.smartrent.domain.auth.dto.EmailCodeSentResponse;
import kr.ac.hansung.smartrent.domain.auth.dto.PasswordResetRequest;
import kr.ac.hansung.smartrent.domain.auth.dto.SignupRequest;
import kr.ac.hansung.smartrent.domain.auth.entity.VerificationPurpose;
import kr.ac.hansung.smartrent.domain.auth.repository.RefreshTokenRepository;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증번호 보내기·회원가입·비밀번호 찾기(B1-05) */
@Service
@RequiredArgsConstructor
public class AccountService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailVerificationService verificationService;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	/**
	 * SIGNUP은 가입되지 않은 학교 이메일, PASSWORD_RESET은 가입된 이메일,
	 * EMAIL_CHANGE는 로그인한 상태에서 다른 사람이 쓰지 않는 새 학교 이메일로 보냄(openapi /auth/email-codes)
	 */
	@Transactional
	public EmailCodeSentResponse sendCode(EmailCodeRequest request) {
		String email = EmailPolicy.requireSchoolEmail(request.email());
		boolean used = userRepository.findByEmail(email).isPresent();
		switch (request.purpose()) {
			case SIGNUP -> {
				if (used) {
					throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
				}
			}
			case PASSWORD_RESET -> {
				if (!used) {
					throw new BusinessException(ErrorCode.NOT_FOUND);
				}
			}
			case EMAIL_CHANGE -> {
				CurrentUser.id();
				if (used) {
					throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
				}
			}
		}
		return new EmailCodeSentResponse(verificationService.issue(email, request.purpose()));
	}

	/** 학생 계정 만들기. 승인 상태는 NONE(학생증 미제출)에서 시작하고 바로 로그인 토큰을 줌 */
	@Transactional
	public AuthTokensResponse signup(SignupRequest request) {
		String email = EmailPolicy.requireSchoolEmail(request.email());
		if (userRepository.findByEmail(email).isPresent()) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
		}
		verificationService.consume(email, VerificationPurpose.SIGNUP, request.code());
		User user = userRepository.save(User.builder()
			.role(Role.STUDENT)
			.email(email)
			.passwordHash(passwordEncoder.encode(request.password()))
			.approval(Approval.NONE)
			.build());
		return tokenService.issue(user);
	}

	/** 인증번호 확인 후 새 비밀번호. 모든 기기의 로그인 유지(리프레시 토큰)를 끊음 */
	@Transactional
	public void resetPassword(PasswordResetRequest request) {
		String email = EmailPolicy.normalize(request.email());
		User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		verificationService.consume(email, VerificationPurpose.PASSWORD_RESET, request.code());
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.deleteAllByUserId(user.getId());
	}
}
