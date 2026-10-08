package kr.ac.hansung.smartrent.domain.equipment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 실제 기기 한 대 (DB 설계 3절 equipment_units) */
@Getter
@Entity
@Table(name = "equipment_units")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EquipmentUnit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "model_id", nullable = false)
	private Long modelId;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "`condition`", nullable = false, length = 12)
	private UnitCondition condition;

	@Column(name = "deleted_at", nullable = true)
	private LocalDateTime deletedAt;
}
