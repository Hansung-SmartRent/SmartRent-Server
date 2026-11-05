package kr.ac.hansung.smartrent.domain.rental;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import org.junit.jupiter.api.Test;
class RentalQueryApiTest extends RentalSupport {
 @Test void B2_04_01_fixture_status_groups() throws Exception {
  mvc.perform(get("/api/v1/rentals/me?group=RENTING").header("Authorization",studentToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
  mvc.perform(get("/api/v1/rentals/me?group=DONE").header("Authorization",studentToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
 }
 @Test void B2_04_02_ownership_and_admin() throws Exception {
  var r=record(model("17"),student(),RentalStatus.RESERVED,"10:00","11:00");
  mvc.perform(get("/api/v1/rentals/"+r.getId()).header("Authorization",token(other(),"STUDENT"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/rentals/"+r.getId()).header("Authorization",adminToken())).andExpect(status().isOk());
  mvc.perform(get("/api/v1/rentals/me").header("Authorization",adminToken())).andExpect(status().isForbidden());
 }
 @Test void B2_04_03_application_group_order() throws Exception {
  clearRentals();
  var m=model("17");
  var r=record(m,student(),RentalStatus.RESERVED,"10:00","11:00");
  em.persist(Rental.builder().applicationId(r.getApplicationId()).userId(student()).modelId(m.getId())
   .status(RentalStatus.RESERVED).startAt(time("15:00")).endAt(time("16:00")).build());
  mvc.perform(get("/api/v1/rentals/me").header("Authorization",studentToken())).andExpect(status().isOk())
   .andExpect(jsonPath("$.data[0].applicationId").value(r.getApplicationId()))
   .andExpect(jsonPath("$.data[1].applicationId").value(r.getApplicationId()));
 }
}
