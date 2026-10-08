package kr.ac.hansung.smartrent.domain.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.hansung.smartrent.global.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 학생·관리자 계정 (DB 설계 3절 users) */
@Getter
@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "role", nullable = false, length = 10)
	private Role role;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "name", nullable = true, length = 30)
	private String name;

	@Column(name = "student_number", nullable = true, length = 20)
	private String studentNumber;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "approval", nullable = false, length = 10)
	private Approval approval;

	@Column(name = "reject_reason", nullable = true, length = 200)
	private String rejectReason;

	@Column(name = "student_id_image_key", nullable = true, length = 200)
	private String studentIdImageKey;

	@Column(name = "profile_image_key", nullable = true, length = 200)
	private String profileImageKey;

	@Column(name = "suspended_until", nullable = true)
	private LocalDateTime suspendedUntil;

	@Column(name = "suspended_until_return", nullable = false)
	private boolean suspendedUntilReturn;

	@Column(name = "suspension_started_by_return", nullable = false)
	private boolean suspensionStartedByReturn;

	/** 새 비밀번호(BCrypt 해시)로 바꿈 */
	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/** 새 학교 이메일(소문자)로 바꿈 */
	public void changeEmail(String email) {
		this.email = email;
	}

	/** 정지 중인지(DB 설계 3절): 반납 전이라 6개월이 아직 시작 안 됐거나, 정지 종료 시각이 지금보다 뒤 */
	public boolean isSuspended(java.time.LocalDateTime now) {
		return suspendedUntilReturn || (suspendedUntil != null && suspendedUntil.isAfter(now));
	}
}
