package kr.ac.hansung.smartrent.domain.auth.repository;

import java.util.Optional;

import kr.ac.hansung.smartrent.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	void deleteByUserId(Long userId);
}
