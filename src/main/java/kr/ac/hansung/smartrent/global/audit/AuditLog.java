package kr.ac.hansung.smartrent.global.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 관리 기록. 서버 자동 처리는 actorId가 비어 있음 (DB 설계 3절 audit_logs) */
@Getter
@Entity
@Table(name = "audit_logs")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLog extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "action", nullable = false, length = 50)
	private String action;

	@Column(name = "detail", nullable = false, length = 500)
	private String detail;

	@Column(name = "actor_id", nullable = true)
	private Long actorId;

	@Column(name = "retain_until", nullable = true)
	private LocalDateTime retainUntil;
}
