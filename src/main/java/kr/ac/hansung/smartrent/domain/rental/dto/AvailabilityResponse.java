package kr.ac.hansung.smartrent.domain.rental.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AvailabilityResponse(LocalDate date, boolean closed, String closedReason,
	List<Slot> slots, LongTerm longTerm, List<LocalDate> multiDayReturnDates) {
	public record Slot(OffsetDateTime start, OffsetDateTime end, int available) { }
	public record LongTerm(LocalDate pickupDate, OffsetDateTime returnDue) { }
}
