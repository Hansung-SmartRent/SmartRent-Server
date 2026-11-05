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
import org.springframework.http.MediaType;
class NotificationSettingsApiTest extends Backend2Support {
 @Autowired NotificationService service;
 @Autowired NotificationRepository notifications;
 @Autowired PushTokenRepository tokens;
 @Autowired NotificationSettingsRepository settings;
 @AfterEach void cleanup() { tokens.deleteAll(); notifications.deleteAll(); settings.deleteAll(); }
 @Test void B2_14_01_02_disabled_inbox_only_then_enabled_push() throws Exception {
  service.register(student(),"settings-device",PushPlatform.ANDROID);
  change(false);
  var n=service.notify(student(),NotificationKind.DUE,"반납 안내","반납 기한 안내",null,null);
  assertThat(notifications.findById(n.getId())).isPresent(); assertThat(push.calls).isZero();
  change(true);
  service.notify(student(),NotificationKind.DUE,"새 기한 안내","반납 기한 변경",null,null);
  assertThat(push.calls).isEqualTo(1);
 }
 @Test void B2_14_03_missing_switch() throws Exception {
  mvc.perform(put("/api/v1/users/me/notification-settings").header("Authorization",studentToken())
   .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
 }
 void change(boolean enabled) throws Exception {
  mvc.perform(put("/api/v1/users/me/notification-settings").header("Authorization",studentToken())
   .contentType(MediaType.APPLICATION_JSON).content("{\"dueEnabled\":"+enabled+"}")).andExpect(status().isOk());
 }
}
