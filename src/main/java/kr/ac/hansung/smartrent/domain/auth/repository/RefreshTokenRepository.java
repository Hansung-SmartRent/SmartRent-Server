package kr.ac.hansung.smartrent.domain.auth.repository;

import java.util.Optional;

import kr.ac.hansung.smartrent.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** 비밀번호를 바꾸거나 탈퇴하면 그 사용자의 리프레시 토큰을 모두 지움(DB 설계 3절) */
	@Modifying
	@Query("delete from RefreshToken t where t.userId = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
}
