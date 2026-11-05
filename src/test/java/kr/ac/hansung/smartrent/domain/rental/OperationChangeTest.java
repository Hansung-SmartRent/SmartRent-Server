package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.operation.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
class OperationChangeTest extends RentalSupport {
 @Autowired OperationChangeService changes;
 @BeforeEach void clean() { clearRentals(); }
 @Test void B2_16_01_OPS_01_OPS_02() {
  var m=model("demo-lg");
  var on7=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  var on8=record(m,other(),RentalStatus.RESERVED,"10:00","11:00");
  on8.changeDeadline(time("11:00").plusDays(1));
  var held=record(m,student(),RentalStatus.RENTING,"09:00","20:00"); held.changeDeadline(time("20:00").plusDays(1));
  em.persist(Holiday.builder().date(LocalDate.of(2026,10,8)).name("임시 휴무").source(HolidaySource.ADMIN).build());
  // on8은 휴무일과 겹치는 다일 예약입니다.
  var result=changes.holidayAdded(LocalDate.of(2026,10,8));
  assertThat(on7.getStatus()).isEqualTo(RentalStatus.RESERVED);
  assertThat(on8.getStatus()).isEqualTo(RentalStatus.CANCELLED);
  assertThat(held.getEndAt()).isEqualTo(LocalDateTime.of(2026,10,12,20,0));
  assertThat(result.extended()).hasSize(1);
 }
 @Test void B2_16_02_UNIT_01_keep_earliest_application() {
  var m=model("demo-lg");
  var a=record(m,student(),RentalStatus.RESERVED,"14:00","16:00");
  var b=record(m,other(),RentalStatus.RESERVED,"14:00","16:00");
  unit(m).changeCondition(UnitCondition.INSPECTION);
  var result=changes.capacityChanged(m.getId());
  assertThat(a.getStatus()).isEqualTo(RentalStatus.RESERVED); assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
  assertThat(result.cancelled()).hasSize(1);
  assertThat(changes.capacityChanged(m.getId()).cancelled()).isEmpty();
 }
 @Test void B2_16_03_hours_cancel_all_outside_regardless_order() {
  var m=model("demo-lg");
  var a=record(m,student(),RentalStatus.RESERVED,"17:00","18:00");
  var b=record(m,other(),RentalStatus.RESERVED,"17:00","18:00");
  var c=record(m,student(),RentalStatus.RESERVED,"16:00","17:00");
  em.persist(OperatingHours.builder().modelId(m.getId()).date(LocalDate.of(2026,10,7)).openTime(LocalTime.of(9,0)).closeTime(LocalTime.of(17,0)).build());
  assertThat(changes.hoursChanged(m.getId(),LocalDate.of(2026,10,7)).cancelled()).hasSize(2);
  assertThat(c.getStatus()).isEqualTo(RentalStatus.RESERVED);
 }
 @Test void B2_16_04_archive_preserves_past() {
  var m=model("17");
  var future=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  var past=record(m,other(),RentalStatus.RETURNED,"07:00","08:00");
  changes.modelArchived(m.getId());
  assertThat(future.getStatus()).isEqualTo(RentalStatus.CANCELLED); assertThat(past.getStatus()).isEqualTo(RentalStatus.RETURNED);
 }
}
