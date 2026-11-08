package kr.ac.hansung.smartrent.domain.operation.dto;

import java.time.LocalDate;

import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import kr.ac.hansung.smartrent.domain.operation.entity.HolidaySource;

/** openapi Holiday */
public record HolidayResponse(LocalDate date, String name, HolidaySource source, boolean openOverride) {

	public static HolidayResponse of(Holiday h) {
		return new HolidayResponse(h.getDate(), h.getName(), h.getSource(), h.isOpenOverride());
	}
}
