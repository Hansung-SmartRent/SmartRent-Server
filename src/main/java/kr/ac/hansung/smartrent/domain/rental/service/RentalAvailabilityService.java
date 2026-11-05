package kr.ac.hansung.smartrent.domain.rental.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalPeriodCalculator;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalSlotCalculator;
import kr.ac.hansung.smartrent.domain.rental.dto.AvailabilityResponse;
import kr.ac.hansung.smartrent.domain.rental.dto.AvailabilityResponse.LongTerm;
import kr.ac.hansung.smartrent.domain.rental.dto.AvailabilityResponse.Slot;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RentalAvailabilityService {
	private final EquipmentModelRepository models;
	private final RentalCalendarReader calendars;
	private final RentalCapacityService capacity;
	private final Clock clock;
	private final RentalPeriodCalculator periods;
	private final RentalSlotCalculator slots = new RentalSlotCalculator();

	public RentalAvailabilityService(EquipmentModelRepository models, RentalCalendarReader calendars,
		RentalCapacityService capacity, Clock clock) {
		this.models = models;
		this.calendars = calendars;
		this.capacity = capacity;
		this.clock = clock.withZone(ZoneId.of("Asia/Seoul"));
		this.periods = new RentalPeriodCalculator(this.clock);
	}

	public AvailabilityResponse availability(long id, LocalDate date) {
		var model = models.findById(id).filter(m -> !m.isArchived())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		var calendar = calendars.read(model);
		if (calendar.isClosed(date)) {
			return new AvailabilityResponse(date, true, calendar.closedReason(date), List.of(), null, List.of());
		}
		var now = LocalDateTime.now(clock);
		var today = now.toLocalDate();
		if (model.getRentalType() == RentalType.LONG_TERM) {
			var selectable = !date.isBefore(today) && !date.isAfter(today.plusYears(1))
				&& now.isBefore(date.atTime(calendar.hours(date).close()));
			return new AvailabilityResponse(date, false, null, List.of(), selectable
				? new LongTerm(date, offset(periods.longTermDeadline(date, calendar))) : null, List.of());
		}
		if (date.isBefore(today) || date.isAfter(periods.nextFriday())) {
			return new AvailabilityResponse(date, false, null, List.of(), null, List.of());
		}
		var deadline = periods.generalDeadline(date, model.getMaxRentalDays(), calendar);
		var availableSlots = slots.slots(date, model.getIntervalMinutes(), calendar).stream()
			.filter(s -> s.start().isAfter(now) && !s.end().isAfter(deadline))
			.filter(s -> model.getMaxRentalMinutes() == null
				|| Duration.between(s.start(), s.end()).toMinutes() <= model.getMaxRentalMinutes())
			.map(s -> new Slot(offset(s.start()), offset(s.end()), capacity.available(model, s))).toList();
		var returnDates = model.isMultiDay() && !availableSlots.isEmpty()
			? date.plusDays(1).datesUntil(deadline.toLocalDate().plusDays(1))
				.filter(d -> !calendar.isClosed(d)).toList()
			: List.<LocalDate>of();
		return new AvailabilityResponse(date, false, null, availableSlots, null, returnDates);
	}

	private static OffsetDateTime offset(LocalDateTime time) {
		return time.atOffset(ZoneOffset.ofHours(9));
	}
}
