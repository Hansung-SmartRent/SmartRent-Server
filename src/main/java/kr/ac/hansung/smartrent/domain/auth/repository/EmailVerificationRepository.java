package kr.ac.hansung.smartrent.domain.auth.repository;

import kr.ac.hansung.smartrent.domain.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
}
