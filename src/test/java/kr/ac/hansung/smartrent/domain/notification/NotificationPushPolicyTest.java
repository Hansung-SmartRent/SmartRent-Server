package kr.ac.hansung.smartrent.domain.notification;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import kr.ac.hansung.smartrent.domain.notification.dto.NotificationResponse;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.repository.*;
import kr.ac.hansung.smartrent.domain.notification.service.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.WarningRepository;
import org.junit.jupiter.api.*;
class NotificationPushPolicyTest {
 final PushSender sender=mock(PushSender.class);
 final PushTokenRepository tokens=mock(PushTokenRepository.class);
 final NotificationSettingsRepository settings=mock(NotificationSettingsRepository.class);
 final WarningRepository warnings=mock(WarningRepository.class);
 final NotificationPushListener listener=new NotificationPushListener(sender,tokens,settings,warnings);
 @BeforeEach void device() {
  when(tokens.findByUserId(1L)).thenReturn(List.of(PushToken.builder().id(1L).userId(1L).token("test-device").build()));
  when(sender.send(anyString(),any())).thenReturn(PushSender.Result.SENT);
 }
 void send(NotificationKind kind,RelatedType type,Long id) {
  listener.created(new NotificationService.NotificationCreated(new NotificationResponse(1L,kind,"안내","본문",type,id,false,
   LocalDateTime.of(2026,10,7,10,0)),1L));
 }
 @Test void B2_13_required_cancellation_push() {
  send(NotificationKind.CANCEL,RelatedType.RENTAL,1L); verify(sender).send(anyString(),any());
 }
 @Test void B2_13_booking_stays_in_inbox() {
  send(NotificationKind.BOOKING,RelatedType.RENTAL,1L); verifyNoInteractions(sender);
 }
 @Test void B2_13_only_no_show_warning_requires_push() {
  when(warnings.findById(2L)).thenReturn(Optional.of(Warning.builder().kind(WarningKind.LATE).build()));
  send(NotificationKind.WARNING,RelatedType.WARNING,2L); verifyNoInteractions(sender);
  when(warnings.findById(3L)).thenReturn(Optional.of(Warning.builder().kind(WarningKind.NO_SHOW).build()));
  send(NotificationKind.WARNING,RelatedType.WARNING,3L); verify(sender).send(anyString(),any());
 }
 @Test void B2_14_due_disabled_does_not_disable_cancellation() {
  when(settings.findById(1L)).thenReturn(Optional.of(NotificationSettings.builder().userId(1L).dueEnabled(false).build()));
  send(NotificationKind.DUE,RelatedType.RENTAL,1L); verifyNoInteractions(sender);
  send(NotificationKind.CANCEL,RelatedType.RENTAL,1L); verify(sender).send(anyString(),any());
 }
}
