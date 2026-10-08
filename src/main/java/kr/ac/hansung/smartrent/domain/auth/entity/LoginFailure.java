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
}
