package kr.ac.hansung.smartrent.global.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

/** created_at·updated_at이 있는 표 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
