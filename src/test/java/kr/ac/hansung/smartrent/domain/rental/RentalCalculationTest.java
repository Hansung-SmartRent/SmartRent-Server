package kr.ac.hansung.smartrent.domain.rental;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.global.exception.*;
import org.junit.jupiter.api.Test;

class RentalCalculationTest {
    final Clock clock = Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    final RentalCapacityCalculator capacity = new RentalCapacityCalculator(clock);
    final RentalPeriodCalculator period = new RentalPeriodCalculator(clock);
    final RentalCalendar calendar = new RentalCalendar() {
        public boolean isClosed(LocalDate d) { return d.getDayOfWeek().getValue() >= 6 || d.equals(LocalDate.of(2026,10,9)); }
        public Hours hours(LocalDate d) { return new Hours(LocalTime.of(9,0), LocalTime.of(17,0)); }
    };
    LocalDateTime at(int hour) { return LocalDate.of(2026,10,7).atTime(hour,0); }
    RentalSegment segment(int a, int b) { return new RentalSegment(at(a),at(b)); }
    EquipmentUnit unit(long id, UnitCondition condition) {
        return EquipmentUnit.builder().id(id).condition(condition).build();
    }
    Rental reserved(int a, int b) { return Rental.builder().status(RentalStatus.RESERVED).startAt(at(a)).endAt(at(b)).build(); }
    @Test void B2_01_01_RSV_02() {
        var units = List.of(unit(1,UnitCondition.NORMAL),unit(2,UnitCondition.NORMAL));
        assertThat(capacity.available(false,units,List.of(reserved(10,12),reserved(12,14)),segment(10,14))).isEqualTo(1);
        assertThat(capacity.available(false,units,List.of(reserved(10,12),reserved(11,13)),segment(10,14))).isZero();
    }
    @Test void B2_01_02_RSV_03() {
        var units=List.of(unit(1,UnitCondition.NORMAL));
        assertThat(capacity.available(true,units,List.of(reserved(14,16)),segment(12,13))).isEqualTo(1);
        assertThat(capacity.available(true,units,List.of(reserved(14,16)),segment(13,14))).isZero();
    }
    @Test void B2_01_03_RSV_06() {
        assertThat(period.nextFriday()).isEqualTo(LocalDate.of(2026,10,16));
        var saturday = new RentalPeriodCalculator(Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"),ZoneOffset.UTC));
        assertThat(saturday.nextFriday()).isEqualTo(LocalDate.of(2026,10,2));
        var deadline=period.generalDeadline(LocalDate.of(2026,10,7),null,calendar);
        assertThatCode(() -> period.checkDeadline(deadline,deadline)).doesNotThrowAnyException();
        assertThatThrownBy(() -> period.checkDeadline(deadline.plusMinutes(1),deadline))
            .isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_BOOKING_WINDOW));
    }
    @Test void B2_01_04_LT_02() {
        assertThat(period.longTermDeadline(LocalDate.of(2026,11,2),calendar)).isEqualTo(LocalDateTime.of(2027,4,30,17,0));
    }
    @Test void B2_01_05_overdue_has_no_end() {
        var rental=Rental.builder().status(RentalStatus.RENTING).unitId(1L).pickedAt(at(7)).endAt(at(8)).build();
        assertThat(capacity.available(false,List.of(unit(1,UnitCondition.NORMAL)),List.of(rental),
            new RentalSegment(at(10).plusYears(2),at(11).plusYears(2)))).isZero();
    }
    @Test void B2_01_07_calendar_days_not_business_days() {
        RentalCalendar monitorCalendar = new RentalCalendar() {
            public boolean isClosed(LocalDate d) { return calendar.isClosed(d); }
            public Hours hours(LocalDate d) { return new Hours(LocalTime.of(9,0),LocalTime.of(20,0)); }
        };
        assertThat(period.generalDeadline(LocalDate.of(2026,10,7),7,monitorCalendar)).isEqualTo(LocalDateTime.of(2026,10,14,20,0));
        assertThatThrownBy(() -> period.checkReturnDate(LocalDate.of(2026,10,9),calendar))
            .isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RETURN_ON_CLOSED_DAY));
    }
    @Test void B2_01_08_disconnected_segments_total_120_minutes() {
        assertThatCode(() -> period.checkMinutes(List.of(segment(10,11),segment(15,16)),120)).doesNotThrowAnyException();
        assertThatThrownBy(() -> period.checkMinutes(List.of(segment(10,13)),120))
            .isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RENTAL_TOO_LONG));
        assertThatThrownBy(() -> period.checkExtensionMinutes(at(10),at(13),120)).isInstanceOf(BusinessException.class);
    }
    @Test void B2_01_09_non_carry_out_has_no_buffer() {
        assertThat(capacity.available(false,List.of(unit(1,UnitCondition.NORMAL)),List.of(reserved(14,15)),segment(13,14))).isEqualTo(1);
    }
    @Test void B2_01_picked_at_and_excluded_units() {
        var rental=Rental.builder().status(RentalStatus.RENTING).unitId(1L).startAt(at(12)).pickedAt(at(10)).endAt(at(16)).build();
        assertThat(capacity.available(false,List.of(unit(1,UnitCondition.NORMAL)),List.of(rental),segment(10,11))).isZero();
        assertThat(capacity.available(false,List.of(unit(1,UnitCondition.INSPECTION),unit(2,UnitCondition.NORMAL)),List.of(rental),segment(12,13))).isEqualTo(1);
    }
    @Test void B2_01_slots_follow_hours_and_closed_days() {
        var slots=new RentalSlotCalculator();
        assertThat(slots.slots(LocalDate.of(2026,10,7),60,calendar)).hasSize(8);
        assertThat(slots.slots(LocalDate.of(2026,10,9),60,calendar)).isEmpty();
        assertThat(slots.slots(LocalDate.of(2026,10,7),30,calendar)).hasSize(16);
    }
}
