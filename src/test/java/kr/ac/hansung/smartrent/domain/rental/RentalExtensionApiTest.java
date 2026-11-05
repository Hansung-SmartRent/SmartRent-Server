package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
class RentalExtensionApiTest extends RentalSupport {
 @BeforeEach void clean() { clearRentals(); at("2026-10-07T11:00:00+09:00"); }
 void extend(Rental r,String end,String error) throws Exception {
  var result=mvc.perform(post("/api/v1/rentals/"+r.getId()+"/extend").header("Authorization",studentToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"newEnd\":\""+end+"+09:00\"}"));
  if(error==null) result.andExpect(status().isOk()).andExpect(jsonPath("$.data.extended").value(true));
  else result.andExpect(jsonPath("$.errorCode").value(error));
 }
 @Test void B2_09_01_EXT_01_once_general_only() throws Exception {
  var r=record(model("1"),student(),RentalStatus.RENTING,"10:00","12:00");
  extend(r,"2026-10-07T21:00:00","EXTENSION_LIMIT"); extend(r,"2026-10-07T20:00:00",null);
  extend(r,"2026-10-07T20:00:00","EXTENSION_NOT_ALLOWED");
  var longTerm=record(model("30"),student(),RentalStatus.RENTING,"09:00","17:00");
  extend(longTerm,"2027-05-07T17:00:00","LONG_TERM_EXTENSION_BLOCKED");
 }
 @Test void B2_09_02_EXT_02_multi_day_limit_and_holiday() throws Exception {
  var r=record(model("demo-lg"),student(),RentalStatus.RENTING,"10:00","20:00"); r.changeDeadline(time("20:00").plusDays(1));
  extend(r,"2026-10-09T20:00:00","EXTENSION_LIMIT"); extend(r,"2026-10-19T20:00:00","EXTENSION_LIMIT");
  extend(r,"2026-10-16T20:00:00",null);
 }
 @Test void B2_09_03_EXT_03_reset_due() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RENTING,"10:00","12:00"); r.markDueNotified();
  extend(r,"2026-10-07T14:00:00",null); assertThat(r.isDueNotified()).isFalse();
 }
 @Test void B2_09_04_past_deadline() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RENTING,"09:30","10:30");
  extend(r,"2026-10-07T12:00:00","EXTENSION_NOT_ALLOWED");
 }
 @Test void B2_09_05_carry_out_buffer() throws Exception {
  var m=model("demo-lg");
  record(m,other(),RentalStatus.RESERVED,"14:00","16:00");
  record(m,other(),RentalStatus.RESERVED,"14:00","16:00");
  var r=record(m,student(),RentalStatus.RENTING,"10:00","12:00");
  extend(r,"2026-10-08T20:00:00","EXTENSION_LIMIT");
 }
 @Test void B2_09_06_seven_days() throws Exception {
  var r=record(model("15"),student(),RentalStatus.RENTING,"10:00","20:00"); r.changeDeadline(time("20:00").plusDays(1));
  extend(r,"2026-10-15T20:00:00","EXTENSION_LIMIT"); extend(r,"2026-10-14T20:00:00",null);
 }
 @Test void B2_09_07_laser_two_hours() throws Exception {
  at("2026-10-07T10:30:00+09:00");
  var r=record(model("29"),student(),RentalStatus.RENTING,"10:00","11:00");
  extend(r,"2026-10-07T13:00:00","EXTENSION_LIMIT"); extend(r,"2026-10-07T12:00:00",null);
 }
}
