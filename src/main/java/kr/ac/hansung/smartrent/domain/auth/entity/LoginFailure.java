package kr.ac.hansung.smartrent.domain.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 연속 실패 횟수와 잠금 시각. 5번 틀리면 5분 막음 (DB 설계 3절 login_failures) */
@Getter
@Entity
@Table(name = "login_failures")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginFailure {

	@Id
	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "fail_count", nullable = false)
	private int failCount;

	@Column(name = "locked_until", nullable = true)
	private LocalDateTime lockedUntil;

	/** 5번 연속 틀리면 5분 동안 막음(기획안 1장, 앱과 같음) */
	public static final int MAX_FAILURES = 5;
	public static final int LOCK_MINUTES = 5;

	public static LoginFailure first(String email) {
		return LoginFailure.builder().email(email).failCount(0).build();
	}

	public boolean isLocked(LocalDateTime now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	public boolean lockExpired(LocalDateTime now) {
		return lockedUntil != null && !lockedUntil.isAfter(now);
	}

	public void recordFailure(LocalDateTime now) {
		failCount++;
		if (failCount >= MAX_FAILURES) {
			lockedUntil = now.plusMinutes(LOCK_MINUTES);
		}
	}
}
