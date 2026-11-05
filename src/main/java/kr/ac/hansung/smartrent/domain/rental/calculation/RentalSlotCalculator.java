package kr.ac.hansung.smartrent.domain.rental.calculation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RentalSlotCalculator {
	public List<RentalSegment> slots(LocalDate date, int intervalMinutes, RentalCalendar calendar) {
		if (intervalMinutes <= 0) throw new IllegalArgumentException("intervalMinutes must be positive");
		if (calendar.isClosed(date)) return List.of();
		var hours = calendar.hours(date);
		var close = date.atTime(hours.close());
		var result = new ArrayList<RentalSegment>();
		for (var start = date.atTime(hours.open()); !start.plusMinutes(intervalMinutes).isAfter(close);
			start = start.plusMinutes(intervalMinutes)) {
			result.add(new RentalSegment(start, start.plusMinutes(intervalMinutes)));
		}
		return List.copyOf(result);
	}
}
