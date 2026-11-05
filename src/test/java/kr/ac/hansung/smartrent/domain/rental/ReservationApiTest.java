package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.ReservationService.*;
import kr.ac.hansung.smartrent.domain.notification.repository.NotificationRepository;
import kr.ac.hansung.smartrent.global.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class ReservationApiTest extends RentalSupport {
 @Autowired NotificationRepository notifications;
 @BeforeEach void clean() { clearRentals(); }
 String segment(String start,String end) { return "{\"start\":\""+start+"+09:00\",\"end\":\""+end+"+09:00\"}"; }
 String request(String key,String... segments) {
  return "{\"modelId\":"+model(key).getId()+",\"segments\":["+String.join(",",segments)+"]}";
 }
 void error(String body,String code) throws Exception {
  mvc.perform(post("/api/v1/reservations").header("Authorization",studentToken()).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.errorCode").value(code));
 }
 void success(String body) throws Exception {
  mvc.perform(post("/api/v1/reservations").header("Authorization",studentToken()).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.data.rentals[0].status").value("RESERVED"));
 }
 @Test void B2_03_01_RSV_01_started_slot() throws Exception {
  at("2026-10-07T10:10:00+09:00");
  error(request("17",segment("2026-10-07T10:00:00","2026-10-07T10:30:00")),"OUT_OF_BOOKING_WINDOW");
 }
 @Test void B2_03_02_RSV_02_03_05_capacity_buffer_closed() throws Exception {
  var m=model("17"); record(m,other(),RentalStatus.RESERVED,"10:00","12:00");
  error(request("17",segment("2026-10-07T10:00:00","2026-10-07T11:00:00")),"NO_CAPACITY");
  error(request("17",segment("2026-10-09T10:00:00","2026-10-09T11:00:00")),"CLOSED_DAY");
 }
 @Test void B2_03_03_04_suspended_pending() throws Exception {
  for(var pair:List.of(new String[]{"taeo@hansung.ac.kr","SUSPENDED"},new String[]{"pending@hansung.ac.kr","NOT_APPROVED"})) {
   var id=users.findByEmail(pair[0]).orElseThrow().getId();
   mvc.perform(post("/api/v1/reservations").header("Authorization",token(id,"STUDENT")).contentType(MediaType.APPLICATION_JSON)
    .content(request("17",segment("2026-10-07T10:00:00","2026-10-07T11:00:00"))))
    .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value(pair[1]));
  }
 }
 @Test void B2_03_05_late_rental() throws Exception {
  record(model("17"),student(),RentalStatus.RENTING,"07:00","08:00");
  error(request("1",segment("2026-10-07T10:00:00","2026-10-07T11:00:00")),"LATE_RENTAL_EXISTS");
 }
 @Test void B2_03_06_closed_return() throws Exception {
  error(request("demo-lg",segment("2026-10-07T10:00:00","2026-10-09T20:00:00")),"RETURN_ON_CLOSED_DAY");
 }
 @Test void B2_03_07_booking_notification() throws Exception {
  success(request("17",segment("2026-10-07T10:00:00","2026-10-07T11:00:00")));
  assertThat(notifications.findAll()).anySatisfy(n->assertThat(n.getKind().name()).isEqualTo("BOOKING"));
 }
 @Test void B2_03_08_09_RSV_04_laser_total_and_same_application() throws Exception {
  error(request("29",segment("2026-10-07T10:00:00","2026-10-07T13:00:00")),"RENTAL_TOO_LONG");
  success(request("29",segment("2026-10-07T10:00:00","2026-10-07T11:00:00"),segment("2026-10-07T15:00:00","2026-10-07T16:00:00")));
  assertThat(rentals.findAll()).extracting(Rental::getApplicationId).containsOnly(rentals.findAll().getFirst().getApplicationId());
  error(request("29",segment("2026-10-07T17:00:00","2026-10-07T18:00:00")),"SAME_KIND_ACTIVE");
 }
 @Test void B2_03_10_11_12_LT_01_format_today_deadline() throws Exception {
  String body="{\"modelId\":"+model("30").getId()+",\"pickupDate\":\"2026-10-07\"}";
  at("2026-10-07T17:00:00+09:00"); error(body,"OUT_OF_BOOKING_WINDOW");
  at("2026-10-07T10:00:00+09:00");
  error("{\"modelId\":"+model("17").getId()+",\"pickupDate\":\"2026-10-07\"}","VALIDATION_ERROR");
  error(request("30",segment("2026-10-07T11:00:00","2026-10-07T12:00:00")),"VALIDATION_ERROR");
  success(body);
  assertThat(rentals.findAll().getFirst().getEndAt()).isEqualTo(LocalDateTime.of(2027,4,7,17,0));
 }
 @Test void B2_03_13_portable_limit() throws Exception {
  error(request("15",segment("2026-10-07T10:00:00","2026-10-15T20:00:00")),"OUT_OF_BOOKING_WINDOW");
  success(request("15",segment("2026-10-07T10:00:00","2026-10-14T20:00:00")));
 }
 @Test void B2_03_14_no_buffer_for_stationary() throws Exception {
  record(model("5"),other(),RentalStatus.RESERVED,"14:00","15:00");
  success(request("5",segment("2026-10-07T13:00:00","2026-10-07T14:00:00")));
 }
 @Test void B2_03_02_RSV_02_peak_not_sum() throws Exception {
  var m=model("29");
  em.createQuery("update EquipmentModel m set m.maxRentalMinutes=null where m.id=:id").setParameter("id",m.getId()).executeUpdate();
  em.refresh(m);
  record(m,other(),RentalStatus.RESERVED,"10:00","12:00");
  var later=record(m,other(),RentalStatus.RESERVED,"12:00","14:00");
  success(request("29",segment("2026-10-07T10:00:00","2026-10-07T14:00:00")));
 }
 @Test void B2_03_02_RSV_03_carry_out_buffer() throws Exception {
  record(model("28"),other(),RentalStatus.RESERVED,"14:00","16:00");
  error(request("28",segment("2026-10-07T13:00:00","2026-10-07T14:00:00")),"NO_CAPACITY");
  success(request("28",segment("2026-10-07T12:00:00","2026-10-07T13:00:00")));
 }
}
