package kr.ac.hansung.smartrent.domain.operation.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 모델별 운영 시간 변경. date가 비면 기본 변경, 있으면 그 날짜만 (DB 설계 3절 operating_hours) */
@Getter
@Entity
@Table(name = "operating_hours")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OperatingHours {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "model_id", nullable = false)
	private Long modelId;

	@Column(name = "date", nullable = true)
	private LocalDate date;

	@Column(name = "open_time", nullable = false)
	private LocalTime openTime;

	@Column(name = "close_time", nullable = false)
	private LocalTime closeTime;
}
