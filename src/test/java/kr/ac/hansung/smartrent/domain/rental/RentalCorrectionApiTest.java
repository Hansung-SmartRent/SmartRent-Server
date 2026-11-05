package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class RentalCorrectionApiTest extends RentalSupport {
 @Autowired WarningService warnings;
 @BeforeEach void clean() { clearRentals(); at("2026-10-07T13:00:00+09:00"); }
 void post(Rental r,String path,String body,int status) throws Exception {
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/rentals/"+r.getId()+"/"+path)
   .header("Authorization",adminToken()).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().is(status));
 }
 @Test void B2_11_01_no_show_correction_only_own_warning() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.NO_SHOW,"10:00","12:00");
  var linked=record(m,student(),RentalStatus.CANCELLED,"15:00","16:00"); warnings.give(r,WarningKind.NO_SHOW,time("10:10"));
  post(r,"correct-no-show","{\"unitCode\":\""+unit(m).getCode()+"\",\"actualPickedAt\":\"2026-10-07T10:05:00+09:00\"}",200);
  assertThat(r.getStatus()).isEqualTo(RentalStatus.RENTING); assertThat(warnings.count(student())).isZero();
  assertThat(linked.getStatus()).isEqualTo(RentalStatus.CANCELLED);
 }
 @Test void B2_11_02_future_pickup_invalid() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.NO_SHOW,"10:00","15:00");
  post(r,"correct-no-show","{\"unitCode\":\""+unit(m).getCode()+"\",\"actualPickedAt\":\"2026-10-07T14:00:00+09:00\"}",400);
 }
 Rental returned() {
  var r=record(model("17"),student(),RentalStatus.RENTING,"10:00","12:00"); r.returnAt(time("12:05"),ReturnCondition.NORMAL);
  warnings.give(r,WarningKind.LATE,time("12:00")); return r;
 }
 @Test void B2_11_03_04_return_correction_revoke_and_restore() throws Exception {
  var r=returned();
  post(r,"correct-return","{\"actualReturnedAt\":\"2026-10-07T11:55:00+09:00\"}",200); assertThat(warnings.count(student())).isZero();
  post(r,"correct-return","{\"actualReturnedAt\":\"2026-10-07T12:10:00+09:00\"}",200); assertThat(warnings.count(student())).isEqualTo(1);
 }
 @Test void B2_11_05_unreported_damage() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RENTING,"10:00","12:00"); r.returnAt(time("11:00"),ReturnCondition.NORMAL);
  post(r,"damage-discovered","{\"note\":\"렌즈 파손\"}",200);
  assertThat(warnings.count(student())).isEqualTo(1); assertThat(em.find(EquipmentUnit.class,r.getUnitId()).getCondition()).isEqualTo(UnitCondition.DAMAGED);
  assertThat(r.getDamageDiscoveredAt()).isEqualTo(time("13:00"));
 }
 @Test void B2_11_06_reported_damage_no_warning() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RENTING,"10:00","12:00"); r.returnAt(time("11:00"),ReturnCondition.NORMAL);
  em.flush(); em.createQuery("update Rental r set r.brokenReported=true where r.id=:id").setParameter("id",r.getId()).executeUpdate(); em.refresh(r);
  post(r,"damage-discovered","{\"note\":\"렌즈 파손\"}",200); assertThat(warnings.count(student())).isZero();
 }
 @Test void B2_11_07_damage_rebalances_future() throws Exception {
  var m=model("demo-lg"); var r=record(m,student(),RentalStatus.RENTING,"10:00","12:00"); r.returnAt(time("11:00"),ReturnCondition.NORMAL);
  var a=record(m,other(),RentalStatus.RESERVED,"15:00","16:00"); var b=record(m,student(),RentalStatus.RESERVED,"15:00","16:00");
  post(r,"damage-discovered","{\"note\":\"렌즈 파손\"}",200);
  assertThat(a.getStatus()).isEqualTo(RentalStatus.RESERVED); assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
 }
}
