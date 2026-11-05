package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.WarningRepository;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class RentalReturnApiTest extends RentalSupport {
 @Autowired RentalJudgmentService judgments;
 @Autowired WarningService warnings;
 @Autowired WarningRepository warningRepository;
 @BeforeEach void clean() { clearRentals(); }
 void returned(Rental r,String condition) throws Exception {
  var code=em.find(EquipmentUnit.class,r.getUnitId()).getCode();
  mvc.perform(post("/api/v1/admin/rentals/"+r.getId()+"/return").header("Authorization",adminToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"unitCode\":\""+code+"\",\"condition\":\""+condition+"\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RETURNED"));
 }
 @Test void B2_08_01_RET_01_no_duplicate_late() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RENTING,"10:00","12:00");
  at("2026-10-07T12:01:00+09:00"); judgments.processModel(m.getId());
  at("2026-10-07T12:05:00+09:00"); returned(r,"NORMAL");
  assertThat(warnings.count(student())).isEqualTo(1); assertThat(r.getReturnedAt()).isEqualTo(time("12:05"));
 }
 @Test void B2_08_02_RET_02_reported_no_damage_warning() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RENTING,"09:30","12:00");
  em.createQuery("update Rental r set r.brokenReported=true where r.id=:id").setParameter("id",r.getId()).executeUpdate(); em.refresh(r);
  at("2026-10-07T11:00:00+09:00"); returned(r,"DAMAGED"); assertThat(warnings.count(student())).isZero();
  var otherRental=record(model("1"),other(),RentalStatus.RENTING,"10:00","12:00");
  returned(otherRental,"DAMAGED"); assertThat(warnings.count(other())).isEqualTo(1);
 }
 @Test void B2_08_03_early_return_frees_unit() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RENTING,"09:30","12:00");
  at("2026-10-07T10:00:00+09:00"); returned(r,"NORMAL");
  assertThat(warnings.count(student())).isZero();
  assertThat(r.getReturnedAt()).isEqualTo(time("10:00"));
 }
 @Test void B2_08_04_last_return_starts_suspension() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RENTING,"09:30","12:00");
  for(int i=0;i<2;i++) warningRepository.save(Warning.builder().userId(student()).kind(WarningKind.DAMAGE).createdAt(time("08:00")).build());
  users.findById(student()).orElseThrow().changeSuspension(null,true,false);
  at("2026-10-07T15:00:00+09:00"); returned(r,"NORMAL");
  assertThat(users.findById(student()).orElseThrow().getSuspendedUntil()).isEqualTo(time("15:00").plusMonths(6));
 }
 @Test void B2_08_05_NS_02_cancel_before_return() throws Exception {
  var m=model("17"); var held=record(m,other(),RentalStatus.RENTING,"09:30","10:00");
  var mine=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T10:20:00+09:00"); returned(held,"NORMAL");
  assertThat(mine.getStatus()).isEqualTo(RentalStatus.CANCELLED); assertThat(warnings.count(student())).isZero();
 }
 @Test void B2_08_06_damage_rebalances_future_reservations() throws Exception {
  var m=model("demo-lg"); var held=record(m,student(),RentalStatus.RENTING,"09:00","12:00");
  var a=record(m,other(),RentalStatus.RESERVED,"15:00","16:00");
  var b=record(m,student(),RentalStatus.RESERVED,"15:00","16:00");
  at("2026-10-07T11:00:00+09:00"); returned(held,"DAMAGED");
  assertThat(a.getStatus()).isEqualTo(RentalStatus.RESERVED); assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
 }
}
