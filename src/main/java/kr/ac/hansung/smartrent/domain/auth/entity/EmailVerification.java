package kr.ac.hansung.smartrent.domain.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.hansung.smartrent.global.entity.BaseCreatedEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 이메일 인증번호(번호는 해시만 저장) (DB 설계 3절 email_verifications) */
@Getter
@Entity
@Table(name = "email_verifications")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailVerification extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "purpose", nullable = false, length = 20)
	private VerificationPurpose purpose;

	@Column(name = "code_hash", nullable = false, length = 100)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "used_at", nullable = true)
	private LocalDateTime usedAt;

	/** 한 번 쓰거나, 다시 보내서 옛 번호가 되면 쓸 수 없게 함 */
	public void markUsed(LocalDateTime now) {
		this.usedAt = now;
	}

	/** 보낸 시각 + 5분까지 유효(그 시각 포함) */
	public boolean isExpired(LocalDateTime now) {
		return now.isAfter(expiresAt);
	}
}
