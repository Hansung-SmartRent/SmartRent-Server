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
 public void changeSuspension(LocalDateTime until,boolean untilReturn,boolean startedByReturn) {
  suspendedUntil=until; suspendedUntilReturn=untilReturn; suspensionStartedByReturn=startedByReturn;
 }

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
	/** 프로필 사진 키 바꾸기. null이면 지움 */
	public void changeProfileImage(String key) {
		this.profileImageKey = key;
	}

	public void changeEmail(String email) {
		this.email = email;
	}

	/** 학생증 제출·재제출(B1-07): 이름·학번·사진 키를 바꾸고 승인 대기로. 반려 사유는 비움 */
	public void submitStudentId(String name, String studentNumber, String imageKey) {
		this.name = name;
		this.studentNumber = studentNumber;
		this.studentIdImageKey = imageKey;
		this.approval = Approval.PENDING;
		this.rejectReason = null;
	}

	/** 관리자의 이름·학번 정정(B1-16, 기획안 11장). 학생 본인은 바꾸지 못함 */
	public void correctNameAndNumber(String name, String studentNumber) {
		this.name = name;
		this.studentNumber = studentNumber;
	}

	/** 가입 승인(B1-15). 학생증 사진 키를 비우고, 비우기 전 키를 돌려줌(지울 대상) */
	public String approve() {
		this.approval = Approval.APPROVED;
		this.rejectReason = null;
		return takeStudentIdImageKey();
	}

	/** 가입 반려(B1-15). 사유를 남기고 학생증 사진 키를 비우며, 비우기 전 키를 돌려줌(지울 대상) */
	public String reject(String reason) {
		this.approval = Approval.REJECTED;
		this.rejectReason = reason;
		return takeStudentIdImageKey();
	}

	private String takeStudentIdImageKey() {
		String key = this.studentIdImageKey;
		this.studentIdImageKey = null;
		return key;
	}

	/** 정지 중인지(DB 설계 3절): 반납 전이라 6개월이 아직 시작 안 됐거나, 정지 종료 시각이 지금보다 뒤 */
	public boolean isSuspended(java.time.LocalDateTime now) {
		return suspendedUntilReturn || (suspendedUntil != null && suspendedUntil.isAfter(now));
	}
}
