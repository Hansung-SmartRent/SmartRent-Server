package kr.ac.hansung.smartrent.domain.auth.repository;

import kr.ac.hansung.smartrent.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
