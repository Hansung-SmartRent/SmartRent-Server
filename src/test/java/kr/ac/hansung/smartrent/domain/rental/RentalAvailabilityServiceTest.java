package kr.ac.hansung.smartrent.domain.rental;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import org.junit.jupiter.api.Test;

class RentalAvailabilityServiceTest {
	final LocalDate date = LocalDate.of(2026, 10, 7);
	final EquipmentModelRepository models = mock(EquipmentModelRepository.class);
	final RentalCalendarReader calendars = mock(RentalCalendarReader.class);
	final RentalCapacityService capacity = mock(RentalCapacityService.class);
	final RentalCalendar calendar = new RentalCalendar() {
		public boolean isClosed(LocalDate d) {
			return d.getDayOfWeek().getValue() >= 6 || d.equals(LocalDate.of(2026, 10, 9));
		}
		public String closedReason(LocalDate d) {
			return d.equals(LocalDate.of(2026, 10, 9)) ? "한글날" : "주말";
		}
		public Hours hours(LocalDate d) { return new Hours(LocalTime.of(9, 30), LocalTime.of(20, 30)); }
	};

	EquipmentModel model(RentalType type, boolean multiDay, Integer days) {
		return EquipmentModel.builder().id(1L).rentalType(type).multiDay(multiDay).maxRentalDays(days)
			.intervalMinutes(30).build();
	}
	RentalAvailabilityService service(EquipmentModel model, String now) {
		when(models.findById(1L)).thenReturn(Optional.of(model));
		when(calendars.read(model)).thenReturn(calendar);
		when(capacity.available(eq(model), any())).thenReturn(1);
		return new RentalAvailabilityService(models, calendars, capacity,
			Clock.fixed(OffsetDateTime.parse(now).toInstant(), ZoneOffset.UTC));
	}
	@Test void B2_02_01_RSV_01() {
		var model = model(RentalType.GENERAL, false, null);
		var morning = service(model, "2026-10-07T08:00:00+09:00").availability(1, date);
		assertThat(morning.slots()).hasSize(22);
		assertThat(morning.slots().getFirst().start()).isEqualTo(OffsetDateTime.parse("2026-10-07T09:30:00+09:00"));
		assertThat(morning.slots().getLast().end()).isEqualTo(OffsetDateTime.parse("2026-10-07T20:30:00+09:00"));
		var later = service(model, "2026-10-07T10:10:00+09:00").availability(1, date);
		assertThat(later.slots()).hasSize(20);
		assertThat(later.slots().getFirst().start().toLocalTime()).isEqualTo(LocalTime.of(10, 30));
		assertThat(service(model, "2026-10-07T10:30:00+09:00").availability(1, date).slots()).hasSize(19);
	}
	@Test void B2_02_02_RSV_05() {
		var service = service(model(RentalType.GENERAL, false, null), "2026-10-07T09:00:00+09:00");
		for (var d : List.of(date.plusDays(2), date.plusDays(3))) {
			var result = service.availability(1, d);
			assertThat(result.closed()).isTrue();
			assertThat(result.closedReason()).isEqualTo(d.equals(date.plusDays(2)) ? "한글날" : "주말");
			assertThat(result.slots()).isEmpty();
		}
		verifyNoInteractions(capacity);
	}
	@Test void B2_02_03_LT_02_and_pickup_boundaries() {
		var model = model(RentalType.LONG_TERM, false, null);
		var service = service(model, "2026-10-07T10:00:00+09:00");
		var result = service.availability(1, LocalDate.of(2026, 11, 2));
		assertThat(result.slots()).isEmpty();
		assertThat(result.longTerm().returnDue()).isEqualTo(OffsetDateTime.parse("2027-04-30T20:30:00+09:00"));
		assertThat(service.availability(1, date).longTerm()).isNotNull();
		assertThat(service.availability(1, date.minusDays(1)).longTerm()).isNull();
		assertThat(service.availability(1, date.plusYears(1)).longTerm()).isNotNull();
		assertThat(service.availability(1, date.plusYears(1).plusDays(1)).longTerm()).isNull();
		assertThat(service(model, "2026-10-07T20:30:00+09:00").availability(1, date).longTerm()).isNull();
	}
	@Test void B2_02_04_demo_lg_RSV_06() {
		var service = service(model(RentalType.GENERAL, true, null), "2026-10-07T08:00:00+09:00");
		assertThat(service.availability(1, date).multiDayReturnDates()).containsExactly(
			date.plusDays(1), date.plusDays(5), date.plusDays(6), date.plusDays(7), date.plusDays(8), date.plusDays(9));
		assertThat(service.availability(1, date.plusDays(12)).slots()).isEmpty();
		assertThat(service.availability(1, date.minusDays(1)).slots()).isEmpty();
	}
	@Test void B2_02_05_portable_monitor_seven_calendar_days() {
		var service = service(model(RentalType.GENERAL, true, 7), "2026-10-07T08:00:00+09:00");
		assertThat(service.availability(1, date).multiDayReturnDates()).containsExactly(
			date.plusDays(1), date.plusDays(5), date.plusDays(6), date.plusDays(7));
	}
	@Test void B2_02_zero_capacity_slots_remain_visible() {
		var model = model(RentalType.GENERAL, false, null);
		var service = service(model, "2026-10-07T08:00:00+09:00");
		when(capacity.available(eq(model), any())).thenReturn(0);
		assertThat(service.availability(1, date).slots()).hasSize(22).allSatisfy(s -> assertThat(s.available()).isZero());
	}
}
