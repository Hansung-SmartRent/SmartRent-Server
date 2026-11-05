package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class WarningApiTest extends RentalSupport {
 @Autowired WarningService warnings;
 @BeforeEach void clean() { clearRentals(); }
 Warning third(boolean held) {
  var m=model("17");
  for(int i=0;i<2;i++) {
   var r=record(m,student(),RentalStatus.RETURNED,"07:00","08:00");
   warnings.give(r,WarningKind.DAMAGE,time("08:00"));
  }
  var r=record(m,student(),held?RentalStatus.RENTING:RentalStatus.RETURNED,"08:00","12:00");
  return warnings.give(r,WarningKind.LATE,time("09:00"));
 }
 @Test void B2_12_01_WARN_01_suspend_until_last_return() {
  var future=record(model("1"),student(),RentalStatus.RESERVED,"15:00","16:00");
  third(true);
  var user=users.findById(student()).orElseThrow();
  assertThat(user.isSuspendedUntilReturn()).isTrue(); assertThat(future.getStatus()).isEqualTo(RentalStatus.CANCELLED);
  var held=rentals.findAll().stream().filter(r->r.getStatus()==RentalStatus.RENTING).findFirst().orElseThrow();
  held.returnAt(time("15:00"),ReturnCondition.NORMAL); warnings.returned(student(),time("15:00"));
  assertThat(user.getSuspendedUntil()).isEqualTo(time("15:00").plusMonths(6));
 }
 @Test void B2_12_02_WARN_02_revoke_unsuspends() throws Exception {
  var w=third(true);
  mvc.perform(post("/api/v1/admin/warnings/"+w.getId()+"/revoke").header("Authorization",adminToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"오판정\"}")).andExpect(status().isOk());
  assertThat(warnings.count(student())).isEqualTo(2);
  assertThat(users.findById(student()).orElseThrow().isSuspended(time("09:00"))).isFalse();
 }
 @Test void B2_12_03_repeated_revoke_rejected() throws Exception {
  var w=third(false); warnings.revoke(w.getId(),"오판정",admin());
  mvc.perform(post("/api/v1/admin/warnings/"+w.getId()+"/revoke").header("Authorization",adminToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"오판정\"}")).andExpect(status().isConflict())
   .andExpect(jsonPath("$.errorCode").value("WARNING_NOT_ACTIVE"));
 }
 @Test void B2_12_04_history_fields() throws Exception {
  third(false);
  mvc.perform(get("/api/v1/users/me/warnings").header("Authorization",studentToken())).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.activeCount").value(3)).andExpect(jsonPath("$.data.items[0].modelName").isNotEmpty())
   .andExpect(jsonPath("$.data.suspension.until").isNotEmpty());
 }
}
