package kr.ac.hansung.smartrent.domain.notification;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.ac.hansung.smartrent.support.Backend2Support;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.repository.*;
import kr.ac.hansung.smartrent.domain.notification.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
class NotificationApiTest extends Backend2Support {
 @Autowired NotificationService service;
 @Autowired NotificationRepository notifications;
 @Autowired PushTokenRepository tokens;
 @BeforeEach void setupToken() { service.register(student(),"test-device",PushPlatform.ANDROID); }
 @AfterEach void cleanup() { tokens.deleteAll(); notifications.deleteAll(); }
 Notification notifyOne() { return service.notify(student(),NotificationKind.DUE,"반납 안내","반납 기한 안내",RelatedType.RENTAL,1L); }
 @Test void B2_13_01_persist_and_push() {
  var n=notifyOne(); assertThat(notifications.findById(n.getId())).isPresent(); assertThat(push.calls).isEqualTo(1);
 }
 @Test void B2_13_02_failure_preserves_inbox() {
  push.result=PushSender.Result.FAILED; var n=notifyOne(); assertThat(notifications.findById(n.getId())).isPresent();
 }
 @Test void B2_13_03_unregistered_removed() {
  push.result=PushSender.Result.UNREGISTERED; notifyOne(); assertThat(tokens.findByToken("test-device")).isEmpty();
 }
 @Test void B2_13_04_read_all() throws Exception {
  notifyOne();
  mvc.perform(post("/api/v1/notifications/read-all").header("Authorization",studentToken())).andExpect(status().isOk());
  mvc.perform(get("/api/v1/notifications").header("Authorization",studentToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].read").value(true));
 }
 @Test void B2_13_05_other_user_cannot_read() throws Exception {
  var n=notifyOne();
  mvc.perform(post("/api/v1/notifications/"+n.getId()+"/read").header("Authorization",token(other(),"STUDENT")))
   .andExpect(status().isNotFound());
 }
 @Test void B2_13_07_related_type_preserved() throws Exception {
  service.answered(student(),"INQUIRY",1L,"문의 답변");
  service.answered(student(),"PURCHASE_REQUEST",1L,"구매 답변");
  mvc.perform(get("/api/v1/notifications").header("Authorization",studentToken()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].relatedType").value("PURCHASE_REQUEST"))
   .andExpect(jsonPath("$.data.items[1].relatedType").value("INQUIRY"));
 }
 @Test void B2_13_logout_removes_only_own_token() {
  service.remove(other(),"test-device"); assertThat(tokens.findByToken("test-device")).isPresent();
  service.remove(student(),"test-device"); assertThat(tokens.findByToken("test-device")).isEmpty();
 }
}
