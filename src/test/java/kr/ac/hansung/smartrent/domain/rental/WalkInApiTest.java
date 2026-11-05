package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.WalkInService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class WalkInApiTest extends RentalSupport {
 @Autowired WalkInService walk;
 @BeforeEach void clean() { clearRentals(); at("2026-10-07T10:05:00+09:00"); }
 void error(String key,String end,String code) throws Exception {
  var m=model(key);
  mvc.perform(post("/api/v1/admin/walk-ins").header("Authorization",adminToken()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"studentNumber\":\"2499001\",\"modelId\":"+m.getId()+",\"unitCode\":\""+unit(m).getCode()+"\",\"end\":\""+end+"+09:00\"}"))
   .andExpect(jsonPath("$.errorCode").value(code));
 }
 @Test void B2_07_01_WALK_01_candidates_and_weekend() {
  var m=model("28"); record(m,other(),RentalStatus.RESERVED,"14:00","15:00");
  assertThat(walk.options("2499001",m.getId()).endOptions()).containsExactly(time("11:00"),time("12:00"),time("13:00"));
  at("2026-10-10T10:05:00+09:00");
  assertThat(walk.options("2499001",m.getId()).endOptions()).isEmpty();
  assertThat(walk.options("2499001",m.getId()).blockReason()).isEqualTo("주말");
 }
 @Test void B2_07_02_invalid_end() throws Exception { error("17","2026-10-07T10:20:00","WALK_IN_END_INVALID"); }
 @Test void B2_07_03_same_kind_active() throws Exception {
  record(model("17"),student(),RentalStatus.RESERVED,"15:00","16:00");
  error("17","2026-10-07T12:00:00","SAME_KIND_ACTIVE");
 }
 @Test void B2_07_04_PICK_02_professor_required() throws Exception { error("30","2027-04-07T17:00:00","PROFESSOR_MAIL_REQUIRED"); }
 @Test void B2_07_05_outside_hours() throws Exception {
  at("2026-10-07T08:00:00+09:00"); error("17","2026-10-07T12:00:00","OUTSIDE_OPERATING_HOURS");
 }
 @Test void B2_07_06_laser_two_hours() {
  assertThat(walk.options("2499001",model("29").getId()).endOptions()).containsExactly(time("11:00"),time("12:00"));
 }
 @Test void B2_07_07_portable_return_dates() {
  assertThat(walk.options("2499001",model("15").getId()).endOptions()).extracting(LocalDateTime::toLocalDate)
   .containsExactly(LocalDate.of(2026,10,8),LocalDate.of(2026,10,12),LocalDate.of(2026,10,13),LocalDate.of(2026,10,14));
 }
 @Test void B2_07_success_directly_renting() throws Exception {
  var m=model("17");
  mvc.perform(post("/api/v1/admin/walk-ins").header("Authorization",adminToken()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"studentNumber\":\"2499001\",\"modelId\":"+m.getId()+",\"unitCode\":\""+unit(m).getCode()+"\",\"end\":\"2026-10-07T12:00:00+09:00\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.data.source").value("WALK_IN")).andExpect(jsonPath("$.data.status").value("RENTING"));
 }
}
