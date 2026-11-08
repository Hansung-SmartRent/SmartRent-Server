package kr.ac.hansung.smartrent.domain.operation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 운영 시간 바꾸기 요청(openapi HoursWrite). date가 비면 기본 변경, 있으면 그 날짜만 */
public record HoursWriteRequest(
	LocalDate date,
	@NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String openTime,
	@NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String closeTime) {
}
