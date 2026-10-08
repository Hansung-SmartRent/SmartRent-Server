package kr.ac.hansung.smartrent.global.storage;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 지울 파일(DB 설계 3절 storage_deletion_jobs). 계정과 묶지 않아 탈퇴·재시작 뒤에도 지울 파일을 잃지 않습니다 */
@Getter
@Entity
@Table(name = "storage_deletion_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageDeletionJob extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "storage_mode", nullable = false, length = 10)
	private StorageMode storageMode;

	@Column(name = "object_key", nullable = false, length = 512)
	private String objectKey;

	@Column(name = "attempted_at", nullable = true)
	private LocalDateTime attemptedAt;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	public StorageDeletionJob(StorageMode storageMode, String objectKey) {
		this.storageMode = storageMode;
		this.objectKey = objectKey;
	}

	public void recordAttempt(LocalDateTime now) {
		this.attemptedAt = now;
		this.attemptCount++;
	}
}
