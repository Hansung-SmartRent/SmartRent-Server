package kr.ac.hansung.smartrent.domain.operation.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import kr.ac.hansung.smartrent.domain.operation.entity.OperatingHours;

/** openapi HoursItem. date가 null이면 기본 변경 */
public record HoursItemResponse(LocalDate date, String openTime, String closeTime) {

	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

	public static HoursItemResponse of(OperatingHours h) {
		return new HoursItemResponse(h.getDate(), h.getOpenTime().format(HM), h.getCloseTime().format(HM));
	}
}
