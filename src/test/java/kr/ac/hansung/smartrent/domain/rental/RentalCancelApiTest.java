package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCancelService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class RentalCancelApiTest extends RentalSupport {
 @Autowired RentalCancelService cancel;
 @BeforeEach void clean() { clearRentals(); }
 @Test void B2_05_01_cancel_restores_capacity_and_alert() throws Exception {
  var m=model("17"); var r=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  mvc.perform(post("/api/v1/rentals/"+r.getId()+"/cancel").header("Authorization",studentToken())).andExpect(status().isOk());
  assertThat(r.getStatus()).isEqualTo(RentalStatus.CANCELLED);
  assertThat(em.createQuery("select count(n) from Notification n where n.kind=kr.ac.hansung.smartrent.domain.notification.entity.NotificationKind.CANCEL",Long.class).getSingleResult()).isEqualTo(1);
 }
 @Test void B2_05_02_started_not_cancellable() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RESERVED,"08:00","10:00");
  mvc.perform(post("/api/v1/rentals/"+r.getId()+"/cancel").header("Authorization",studentToken()))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("NOT_CANCELLABLE"));
 }
 @Test void B2_05_03_shared_cancel_without_warning() {
  var r=record(model("17"),student(),RentalStatus.RESERVED,"10:00","11:00");
  cancel.cancel(r,"예약 시작 시 기기 부족"); cancel.cancel(r,"예약 시작 시 기기 부족");
  assertThat(r.getStatus()).isEqualTo(RentalStatus.CANCELLED);
  assertThat(r.getCancelReason()).isEqualTo("예약 시작 시 기기 부족");
 }
 @Test void B2_05_04_admin_audit() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RESERVED,"08:00","10:00");
  mvc.perform(post("/api/v1/admin/rentals/"+r.getId()+"/cancel").header("Authorization",adminToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"학생 요청\"}")).andExpect(status().isOk());
  assertThat(em.createQuery("select count(a) from AuditLog a where a.action='CANCEL'",Long.class).getSingleResult()).isEqualTo(1);
 }
}
