package kr.ac.hansung.smartrent.domain.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.auth.entity.EmailVerification;
import kr.ac.hansung.smartrent.domain.auth.entity.VerificationPurpose;
import kr.ac.hansung.smartrent.domain.auth.repository.EmailVerificationRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.mail.CodeMailer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증번호(DB 설계 3절 email_verifications). 번호는 해시만 저장하고 5분 동안 유효합니다.
 * 다시 보내면 같은 이메일·목적의 옛 번호는 못 씁니다. 메일 보내기에 실패하면 이 처리 전체가 되돌려져 그 번호도 남지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	public static final int VALID_MINUTES = 5;

	private final EmailVerificationRepository repository;
	private final VerificationCodeGenerator generator;
	private final PasswordEncoder passwordEncoder;
	private final CodeMailer mailer;
	private final Clock clock;

	/** 새 번호를 만들어 보내고 만료 시각을 돌려줌. email은 이미 소문자·학교 도메인 확인을 마친 값 */
	@Transactional
	public LocalDateTime issue(String email, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now(clock);
		repository.findByEmailAndPurposeAndUsedAtIsNull(email, purpose).forEach(old -> old.markUsed(now));
		String code = generator.next();
		EmailVerification saved = repository.save(EmailVerification.builder()
			.email(email)
			.purpose(purpose)
			.codeHash(passwordEncoder.encode(code))
			.expiresAt(now.plusMinutes(VALID_MINUTES))
			.build());
		mailer.send(email, code);
		return saved.getExpiresAt();
	}

	/**
	 * 번호가 맞고 5분 안이면 사용 처리. 틀리거나 이미 썼거나 다른 이메일이면 OTP_INVALID, 맞지만 5분이 지났으면 OTP_EXPIRED(사례 AUTH-02)
	 */
	@Transactional
	public void consume(String email, VerificationPurpose purpose, String code) {
		LocalDateTime now = LocalDateTime.now(clock);
		EmailVerification latest = repository.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByIdDesc(email, purpose)
			.filter(v -> passwordEncoder.matches(code, v.getCodeHash()))
			.orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID));
		if (latest.isExpired(now)) {
			throw new BusinessException(ErrorCode.OTP_EXPIRED);
		}
		latest.markUsed(now);
	}
}
