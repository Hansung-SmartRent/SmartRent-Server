package kr.ac.hansung.smartrent.domain.rental.calculation;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;

public final class RentalPeriodCalculator {
	private final Clock clock;
	public RentalPeriodCalculator(Clock clock) {
		this.clock = clock.withZone(ZoneId.of("Asia/Seoul"));
	}

	public LocalDate nextFriday() {
		return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(11);
	}

	public LocalDateTime generalDeadline(LocalDate start, Integer maxRentalDays, RentalCalendar calendar) {
		LocalDate date = maxRentalDays == null ? nextFriday() : start.plusDays(maxRentalDays);
		return date.atTime(calendar.hours(date).close());
	}

	public LocalDateTime longTermDeadline(LocalDate pickup, RentalCalendar calendar) {
		LocalDate date = pickup.plusMonths(6);
		while (calendar.isClosed(date)) date = date.minusDays(1);
		return date.atTime(calendar.hours(date).close());
	}

	public void checkReturnDate(LocalDate date, RentalCalendar calendar) {
		if (calendar.isClosed(date)) throw new BusinessException(ErrorCode.RETURN_ON_CLOSED_DAY);
	}

	public void checkDeadline(LocalDateTime end, LocalDateTime deadline) {
		if (end.isAfter(deadline)) throw new BusinessException(ErrorCode.OUT_OF_BOOKING_WINDOW);
	}

	public void checkMinutes(List<RentalSegment> segments, Integer maxMinutes) {
		if (segments.isEmpty()) throw new BusinessException(ErrorCode.SEGMENT_INVALID);
		Duration total = segments.stream().map(s -> Duration.between(s.start(), s.end()))
			.reduce(Duration.ZERO, Duration::plus);
		if (maxMinutes != null && total.compareTo(Duration.ofMinutes(maxMinutes)) > 0)
			throw new BusinessException(ErrorCode.RENTAL_TOO_LONG);
	}

	public void checkExtensionMinutes(LocalDateTime rentalStart, LocalDateTime end, Integer maxMinutes) {
		checkMinutes(List.of(new RentalSegment(rentalStart, end)), maxMinutes);
	}
}
