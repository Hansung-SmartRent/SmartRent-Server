package kr.ac.hansung.smartrent.domain.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import kr.ac.hansung.smartrent.domain.user.entity.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.*;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.warning.service.RetainedWarningService;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

class RetainedWarningIntegrationTest extends RentalSupport {
 @Autowired WarningRepository warnings;
 @Autowired RetainedWarningRepository retained;
 @Autowired WarningService warningService;
 @Autowired RetainedWarningService service;

 @BeforeEach void clean() {
  clearRentals();
  warnings.deleteAll();
  retained.deleteAll();
  at("2026-10-07T10:00:00+09:00");
 }

 private Long withdrawAndRejoin(LocalDateTime suspendedUntil) throws Exception {
  var original=users.findById(student()).orElseThrow();
  var email=original.getEmail();
  var passwordHash=original.getPasswordHash();
  original.changeSuspension(suspendedUntil,false,false);
  for(int i=0;i<3;i++) warnings.save(Warning.builder().userId(original.getId())
   .kind(WarningKind.DAMAGE).createdAt(time("08:00").minusDays(i)).build());
  em.flush();
  mvc.perform(delete("/api/v1/users/me").header("Authorization",studentToken()))
   .andExpect(status().isOk());
  em.flush(); em.clear();
  assertThat(users.findByEmail(email)).isEmpty();
  assertThat(retained.findByStudentNumber("2499001")).hasSize(3);
  var rejoined=users.saveAndFlush(User.builder().email(email).passwordHash(passwordHash)
   .role(Role.STUDENT).approval(Approval.NONE).build());
  rejoined.submitStudentId("김하늘","2499001",null);
  em.flush();
  assertThat(warningService.count(rejoined.getId())).isZero();
  assertThat(rejoined.isSuspended(time("10:00"))).isFalse();
  return rejoined.getId();
 }

 @Test void B2_17_01_02_04_AUTH_03_approval_restores_to_new_account() throws Exception {
  var until=LocalDateTime.of(2026,11,1,9,0);
  var id=withdrawAndRejoin(until);
  mvc.perform(post("/api/v1/admin/students/"+id+"/approve").header("Authorization",adminToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.activeWarningCount").value(3))
   .andExpect(jsonPath("$.data.suspension.suspended").value(true));
  em.flush(); em.clear();
  assertThat(users.findById(id).orElseThrow().getSuspendedUntil()).isEqualTo(until);
  assertThat(warningService.count(id)).isEqualTo(3);
  assertThat(retained.findByStudentNumber("2499001")).isEmpty();
  service.restore(id,"2499001");
  assertThat(warnings.findByUserIdOrderByCreatedAtDescIdDesc(id)).hasSize(3);
  mvc.perform(post("/api/v1/admin/students/"+id+"/approve").header("Authorization",adminToken()))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("APPROVAL_NOT_PENDING"));
  assertThat(warnings.findByUserIdOrderByCreatedAtDescIdDesc(id)).hasSize(3);
 }

 @Test void B2_17_05_approval_after_original_suspension_does_not_revive_it() throws Exception {
  var until=LocalDateTime.of(2026,11,1,9,0);
  var id=withdrawAndRejoin(until);
  at("2026-11-01T09:00:00+09:00");
  mvc.perform(post("/api/v1/admin/students/"+id+"/approve").header("Authorization",adminToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.activeWarningCount").value(0))
   .andExpect(jsonPath("$.data.suspension.suspended").value(false));
  assertThat(warnings.findByUserIdOrderByCreatedAtDescIdDesc(id)).allSatisfy(w->
   assertThat(w.getExpiresAt()).isEqualTo(until));
 }

 @Test void B2_17_05_approval_at_retention_deadline_does_not_restore() throws Exception {
  var id=withdrawAndRejoin(LocalDateTime.of(2026,11,1,9,0));
  at("2027-04-07T10:00:00+09:00");
  mvc.perform(post("/api/v1/admin/students/"+id+"/approve").header("Authorization",adminToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.activeWarningCount").value(0))
   .andExpect(jsonPath("$.data.suspension.suspended").value(false));
  assertThat(warnings.findByUserIdOrderByCreatedAtDescIdDesc(id)).isEmpty();
  assertThat(retained.findByStudentNumber("2499001")).isEmpty();
 }
}
