package kr.ac.hansung.smartrent.domain.rental.calculation;

import java.time.LocalDate;
import java.time.LocalTime;

public interface RentalCalendar {
	boolean isClosed(LocalDate date);
	Hours hours(LocalDate date);
	default String closedReason(LocalDate date) {
		return date.getDayOfWeek().getValue() >= 6 ? "주말" : null;
	}

	record Hours(LocalTime open, LocalTime close) { }
}
