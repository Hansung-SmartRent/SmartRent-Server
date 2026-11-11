package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.*;
import kr.ac.hansung.smartrent.domain.warning.service.*;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
class RetainedWarningTest extends RentalSupport {
 @Autowired RetainedWarningService service;
 @Autowired WarningRepository warnings;
 @Autowired RetainedWarningRepository retained;
 @Autowired WarningService warningService;
 @Autowired AuditLogService audit;
 @BeforeEach void clean() { clearRentals(); }
 @Test void B2_17_01_AUTH_03_retain_six_months() {
  warnings.save(Warning.builder().userId(student()).kind(WarningKind.DAMAGE).createdAt(time("08:00")).build());
  at("2026-10-07T10:00:00+09:00"); service.retain(student());
  assertThat(retained.findByStudentNumber("2499001")).singleElement().satisfies(w->
   assertThat(w.getRetainUntil()).isEqualTo(time("10:00").plusMonths(6)));
  assertThat(warningService.count(student())).isZero();
 }
 @Test void B2_17_02_04_restore_original_end_not_new_six_months() {
  var until=time("09:00").withMonth(11).withDayOfMonth(1);
  retained.save(RetainedWarning.builder().studentNumber("2499001").kind(WarningKind.DAMAGE)
   .createdAt(time("08:00")).originalSuspendedUntil(until).retainUntil(time("09:00").plusMonths(6)).build());
  service.restore(student(),"2499001");
  assertThat(users.findById(student()).orElseThrow().getSuspendedUntil()).isEqualTo(until);
  assertThat(warningService.count(student())).isEqualTo(1);
 }
 @Test void B2_17_03_purge_and_anonymize() {
  var expiry=time("09:00").minusDays(1);
  retained.save(RetainedWarning.builder().studentNumber("2499001").kind(WarningKind.DAMAGE)
   .createdAt(time("08:00").minusMonths(7)).retainUntil(expiry).build());
  audit.record("WITHDRAW","탈퇴 학번 2499001",null,expiry);
  service.purgeExpired(); em.flush(); em.clear();
  assertThat(retained.findByStudentNumber("2499001")).isEmpty();
  assertThat(em.createQuery("select a.detail from AuditLog a where a.action='WITHDRAW'",String.class).getResultList())
   .containsOnly("탈퇴 회원");
 }
 @Test void B2_17_05_expired_and_idempotent_restore() {
  retained.save(RetainedWarning.builder().studentNumber("2499001").kind(WarningKind.DAMAGE)
   .createdAt(time("08:00").minusMonths(7)).originalSuspendedUntil(time("08:00").minusDays(1))
   .retainUntil(time("09:00").plusMonths(1)).build());
  service.restore(student(),"2499001"); service.restore(student(),"2499001");
  assertThat(warnings.findByUserIdOrderByCreatedAtDescIdDesc(student())).hasSize(1);
  assertThat(warningService.count(student())).isZero();
  assertThat(users.findById(student()).orElseThrow().isSuspended(time("09:00"))).isFalse();
 }
 @Test void B2_17_03_purge_keeps_records_until_exact_deadline() {
  var deadline=time("10:00").plusMonths(6);
  retained.save(RetainedWarning.builder().studentNumber("2499001").kind(WarningKind.DAMAGE)
   .createdAt(time("08:00")).retainUntil(deadline).build());
  audit.record("WITHDRAW","탈퇴 학번 2499001",null,deadline);
  at("2027-04-07T09:59:59+09:00");
  service.purgeExpired(); em.flush(); em.clear();
  assertThat(retained.findByStudentNumber("2499001")).hasSize(1);
  assertThat(em.createQuery("select a.detail from AuditLog a where a.action='WITHDRAW'",String.class)
   .getResultList()).contains("탈퇴 학번 2499001");
  at("2027-04-07T10:00:00+09:00");
  service.purgeExpired(); em.flush(); em.clear();
  assertThat(retained.findByStudentNumber("2499001")).isEmpty();
  assertThat(em.createQuery("select a.detail from AuditLog a where a.action='WITHDRAW'",String.class)
   .getResultList()).containsOnly("탈퇴 회원");
 }
}
