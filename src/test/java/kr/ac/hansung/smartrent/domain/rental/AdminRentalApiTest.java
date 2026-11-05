package kr.ac.hansung.smartrent.domain.rental;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.Test;
class AdminRentalApiTest extends RentalSupport {
 @Test void B2_15_01_fixture_summary() throws Exception {
  mvc.perform(get("/api/v1/admin/summary").header("Authorization",adminToken())).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.renting").value(2)).andExpect(jsonPath("$.data.pickupWaiting").value(0))
   .andExpect(jsonPath("$.data.pendingStudents").value(1));
 }
 @Test void B2_15_02_status_and_unit_search() throws Exception {
  mvc.perform(get("/api/v1/admin/rentals?status=RENTING").header("Authorization",adminToken())).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.items.length()").value(2)).andExpect(jsonPath("$.data.items[0].student.id").exists())
   .andExpect(jsonPath("$.data.items[0].brokenNote").hasJsonPath());
  mvc.perform(get("/api/v1/admin/rentals?q=36").header("Authorization",adminToken())).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.items[0].unit.code").value("36"));
 }
 @Test void B2_15_03_paging_and_permissions() throws Exception {
  mvc.perform(get("/api/v1/admin/rentals?page=0&size=2").header("Authorization",adminToken())).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.items.length()").value(2)).andExpect(jsonPath("$.data.page.totalElements").value(7));
  mvc.perform(get("/api/v1/admin/rentals").header("Authorization",studentToken())).andExpect(status().isForbidden());
 }
}
