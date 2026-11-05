package kr.ac.hansung.smartrent.domain.notification.service;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationKind;
import kr.ac.hansung.smartrent.domain.notification.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;
@Slf4j @Component @RequiredArgsConstructor
public class NotificationPushListener {
 private final PushSender sender;
 private final PushTokenRepository tokens;
 private final NotificationSettingsRepository settings;
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void created(NotificationService.NotificationCreated event) {
  if(event.notification().kind()==NotificationKind.DUE &&
   !settings.findById(event.userId()).map(s->s.isDueEnabled()).orElse(true)) return;
  for(var token:tokens.findByUserId(event.userId())) {
   try {
    var result=sender.send(token.getToken(),event.notification());
    if(result==PushSender.Result.UNREGISTERED) tokens.delete(token);
    else if(result==PushSender.Result.FAILED) log.warn("푸시 발송 실패: 알림 id {}",event.notification().id());
   } catch(RuntimeException ex) { log.warn("푸시 발송 실패: 알림 id {}",event.notification().id()); }
  }
 }
}
