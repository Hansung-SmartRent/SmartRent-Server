package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.WarningRepository;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
class RentalJudgmentTest extends RentalSupport {
 @Autowired RentalJudgmentService judgments;
 @Autowired WarningRepository warnings;
 @Autowired WarningService warningService;
 @Autowired NotificationRepository notifications;
 @BeforeEach void clean() { clearRentals(); }
 @Test void B2_10_01_NS_01_linked_remaining_cancelled() {
  var m=model("17"); var r=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  var linked=Rental.builder().applicationId(r.getApplicationId()).userId(student()).modelId(m.getId())
   .status(RentalStatus.RESERVED).startAt(time("15:00")).endAt(time("16:00")).build(); em.persist(linked);
  at("2026-10-07T10:09:00+09:00"); judgments.processModel(m.getId()); assertThat(r.getStatus()).isEqualTo(RentalStatus.RESERVED);
  at("2026-10-07T10:10:00+09:00"); judgments.processModel(m.getId()); assertThat(r.getStatus()).isEqualTo(RentalStatus.NO_SHOW);
  assertThat(linked.getStatus()).isEqualTo(RentalStatus.CANCELLED); assertThat(linked.getCancelReason()).isEqualTo("연결 신청 미수령");
  assertThat(warningService.count(student())).isEqualTo(1);
 }
 @Test void B2_10_02_NS_02_historical_shortage_before_no_show() {
  var m=model("17"); var held=record(m,other(),RentalStatus.RENTING,"09:00","10:00");
  var r=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  held.returnAt(time("10:20"),ReturnCondition.NORMAL);
  at("2026-10-07T10:21:00+09:00"); judgments.processModel(m.getId()); judgments.processModel(m.getId());
  assertThat(r.getStatus()).isEqualTo(RentalStatus.CANCELLED); assertThat(r.getCancelReason()).isEqualTo("예약 시작 시 기기 부족");
  assertThat(warningService.count(student())).isZero();
 }
 @Test void B2_10_03_EXT_03_reset_due_after_extension() {
  var m=model("17"); var r=record(m,student(),RentalStatus.RENTING,"10:00","12:00");
  at("2026-10-07T11:46:00+09:00"); judgments.processModel(m.getId()); assertThat(r.isDueNotified()).isTrue();
  r.extend(time("14:00"));
  at("2026-10-07T13:30:00+09:00"); judgments.processModel(m.getId()); assertThat(r.isDueNotified()).isFalse();
  at("2026-10-07T13:46:00+09:00"); judgments.processModel(m.getId()); assertThat(r.isDueNotified()).isTrue();
  assertThat(notifications.findAll().stream().filter(n->n.getKind().name().equals("DUE")).count()).isEqualTo(2);
 }
 @Test void B2_10_04_05_deadline_timestamp_idempotence() {
  var m=model("17"); var r=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  at("2026-10-07T10:10:40+09:00"); judgments.processModel(m.getId()); judgments.processModel(m.getId());
  assertThat(warnings.findByRentalIdAndKind(r.getId(),WarningKind.NO_SHOW)).get().extracting(Warning::getCreatedAt).isEqualTo(time("10:10"));
  assertThat(warningService.count(student())).isEqualTo(1);
 }
 @Test void B2_10_06_catch_up_all_due_work() {
  var m=model("demo-lg"); var a=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  var held=record(model("17"),other(),RentalStatus.RENTING,"09:00","10:00");
  at("2026-10-07T11:10:00+09:00"); judgments.processModel(m.getId()); judgments.processModel(held.getModelId());
  assertThat(a.getStatus()).isEqualTo(RentalStatus.NO_SHOW);
  assertThat(warnings.findByRentalIdAndKind(held.getId(),WarningKind.LATE)).get().extracting(Warning::getCreatedAt).isEqualTo(time("10:00"));
 }
 @Test void B2_10_07_long_and_multi_day_due() {
  var longModel=model("30"); var a=record(longModel,student(),RentalStatus.RENTING,"09:00","17:00"); a.changeDeadline(time("17:00").plusDays(7));
  var multi=model("demo-lg"); var b=record(multi,other(),RentalStatus.RENTING,"09:00","17:00"); b.changeDeadline(time("17:00").plusDays(1));
  at("2026-10-07T17:00:00+09:00"); judgments.processModel(longModel.getId()); judgments.processModel(multi.getId());
  assertThat(a.isDueNotified()).isTrue(); assertThat(b.isDueNotified()).isTrue();
 }
 @Test void B2_10_08_expire_suspension_and_warnings() {
  var user=users.findByEmail("taeo@hansung.ac.kr").orElseThrow();
  user.changeSuspension(time("08:00"),false,false);
  warningService.expireSuspensions(); assertThat(warningService.count(user.getId())).isZero();
  assertThat(user.getSuspendedUntil()).isNull();
 }
}
