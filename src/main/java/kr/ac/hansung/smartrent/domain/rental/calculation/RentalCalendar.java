package kr.ac.hansung.smartrent.domain.rental.calculation;

import java.time.LocalDate;
import java.time.LocalTime;

public interface RentalCalendar {
	boolean isClosed(LocalDate date);
	Hours hours(LocalDate date);

	record Hours(LocalTime open, LocalTime close) { }
}
