package kr.ac.hansung.smartrent.domain.equipment.entity;

import java.time.LocalTime;

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

/** 기자재 모델(예약 단위) (DB 설계 3절 equipment_models) */
@Getter
@Entity
@Table(name = "equipment_models")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EquipmentModel extends BaseTimeEntity {

	/** 운영 중인 모델끼리 이름이 겹치는지 볼 때 쓰는 값: 소문자로 바꾸고 공백을 모두 뺌(DB 설계 3절 name_key) */
	public static String nameKeyOf(String name) {
		return name.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "source", nullable = false, length = 10)
	private ModelSource source;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "example_for", nullable = true, length = 10)
	private ExampleFor exampleFor;

	@Column(name = "model_key", nullable = false, length = 20)
	private String modelKey;

	@Column(name = "site_number", nullable = true, length = 10)
	private String siteNumber;

	@Column(name = "category", nullable = false, length = 20)
	private String category;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "name_key", nullable = true, length = 100)
	private String nameKey;

	@Column(name = "manufacturer", nullable = true, length = 50)
	private String manufacturer;

	@Column(name = "purchase_year", nullable = true, length = 4)
	private String purchaseYear;

	@Column(name = "purpose", nullable = true, length = 200)
	private String purpose;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "rental_type", nullable = false, length = 10)
	private RentalType rentalType;

	@Column(name = "multi_day", nullable = false)
	private boolean multiDay;

	@Column(name = "max_rental_days", nullable = true)
	private Integer maxRentalDays;

	@Column(name = "max_rental_minutes", nullable = true)
	private Integer maxRentalMinutes;

	@Column(name = "carry_out", nullable = false)
	private boolean carryOut;

	@Column(name = "location", nullable = false, length = 100)
	private String location;

	@Column(name = "contact", nullable = true, length = 30)
	private String contact;

	@Column(name = "open_time", nullable = false)
	private LocalTime openTime;

	@Column(name = "close_time", nullable = false)
	private LocalTime closeTime;

	@Column(name = "interval_minutes", nullable = false)
	private int intervalMinutes;

	@Column(name = "guide", nullable = true, columnDefinition = "TEXT")
	private String guide;

	@Column(name = "archived", nullable = false)
	private boolean archived;
}
