package kr.ac.hansung.smartrent.domain.auth.repository;

import java.util.List;
import java.util.Optional;

import kr.ac.hansung.smartrent.domain.auth.entity.EmailVerification;
import kr.ac.hansung.smartrent.domain.auth.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	/** 아직 쓰지 않은 번호들(다시 보낼 때 무효로 만들 대상) */
	List<EmailVerification> findByEmailAndPurposeAndUsedAtIsNull(String email, VerificationPurpose purpose);

	/** 가장 최근에 보낸, 아직 쓰지 않은 번호 */
	Optional<EmailVerification> findFirstByEmailAndPurposeAndUsedAtIsNullOrderByIdDesc(String email, VerificationPurpose purpose);
}
