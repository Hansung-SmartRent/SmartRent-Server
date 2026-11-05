package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class RentalPickupApiTest extends RentalSupport {
 @Autowired WarningService warnings;
 @BeforeEach void clean() { clearRentals(); }
 void pickup(Rental r,String code,boolean checked,String error) throws Exception {
  var result=mvc.perform(post("/api/v1/admin/rentals/"+r.getId()+"/pickup").header("Authorization",adminToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"unitCode\":\""+code+"\",\"professorMailChecked\":"+checked+"}"));
  if(error==null) result.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RENTING"));
  else result.andExpect(jsonPath("$.errorCode").value(error));
 }
 @Test void B2_06_01_PICK_01_operating_and_early_pickup() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T08:30:00+09:00"); pickup(r,unit(m).getCode(),false,"OUTSIDE_OPERATING_HOURS");
  at("2026-10-07T09:35:00+09:00"); pickup(r,unit(m).getCode(),false,null);
  assertThat(r.getEndAt()).isEqualTo(time("12:00")); assertThat(r.getPickedAt()).isEqualTo(time("09:35"));
 }
 @Test void B2_06_02_PICK_02_professor_required() throws Exception {
  var m=model("30"); var r=record(m,student(),RentalStatus.RESERVED,"09:00","17:00"); r.changeDeadline(time("17:00").plusMonths(6));
  pickup(r,unit(m).getCode(),false,"PROFESSOR_MAIL_REQUIRED"); pickup(r,unit(m).getCode(),true,null);
 }
 @Test void B2_06_03_boundary_before_deadline() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T10:09:59+09:00"); pickup(r,unit(m).getCode(),false,null);
 }
 @Test void B2_06_04_boundary_at_deadline() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T10:10:00+09:00"); pickup(r,unit(m).getCode(),false,"NO_SHOW_DEADLINE_PASSED");
  assertThat(r.getStatus()).isEqualTo(RentalStatus.NO_SHOW); assertThat(warnings.count(student())).isEqualTo(1);
 }
 @Test void B2_06_05_other_model_unit() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T09:35:00+09:00"); pickup(r,unit(model("1")).getCode(),false,"UNIT_MISMATCH");
 }
 @Test void B2_06_06_unit_in_use() throws Exception {
  var m=model("demo-lg"); record(m,other(),RentalStatus.RENTING,"09:00","12:00");
  var r=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T09:35:00+09:00"); pickup(r,unit(m).getCode(),false,"UNIT_IN_USE");
 }
 @Test void B2_06_07_early_capacity() throws Exception {
  var m=model("17"); record(m,other(),RentalStatus.RESERVED,"09:30","10:00");
  var r=record(m,student(),RentalStatus.RESERVED,"10:00","12:00");
  at("2026-10-07T09:35:00+09:00"); pickup(r,unit(m).getCode(),false,"NO_EARLY_CAPACITY");
 }
 @Test void B2_06_08_long_term_close_boundary_and_lookup() throws Exception {
  var m=model("30"); var r=record(m,student(),RentalStatus.RESERVED,"09:00","17:00"); r.changeDeadline(time("17:00").plusMonths(6));
  mvc.perform(get("/api/v1/admin/pickups/lookup?studentNumber=2499001").header("Authorization",adminToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(r.getId()))
   .andExpect(jsonPath("$.data[0].student.studentNumber").value("2499001"));
  at("2026-10-07T17:00:00+09:00"); pickup(r,unit(m).getCode(),true,"NO_SHOW_DEADLINE_PASSED");
  assertThat(r.getStatus()).isEqualTo(RentalStatus.NO_SHOW);
 }
}
