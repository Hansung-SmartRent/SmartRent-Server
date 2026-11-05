package kr.ac.hansung.smartrent.domain.notification.service;
import java.time.*;
import kr.ac.hansung.smartrent.domain.notification.dto.NotificationResponse;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.repository.*;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.*;
import kr.ac.hansung.smartrent.global.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j @Service @RequiredArgsConstructor
@Transactional
public class NotificationService implements OperationsAlert, AccountAlert, NoticeAlert, BoardAlert, PushTokenRemover {
 private final NotificationRepository notifications;
 private final PushTokenRepository tokens;
 private final UserRepository users;
 private final Clock clock;
 private final ApplicationEventPublisher events;
 public Notification notify(Long userId,NotificationKind kind,String title,String body,RelatedType type,Long relatedId) {
  if((type==null)!=(relatedId==null)) throw new IllegalArgumentException("알림 연결 종류와 번호는 함께 지정합니다.");
  var saved=notifications.save(Notification.builder().userId(userId).kind(kind).title(title).body(body)
   .relatedType(type).relatedId(relatedId).createdAt(LocalDateTime.now(clock)).build());
  events.publishEvent(new NotificationCreated(NotificationResponse.of(saved),userId));
  return saved;
 }
 public void toAdmins(NotificationKind kind,String title,String body,RelatedType type,Long id) {
  users.findAllByRole(Role.ADMIN).forEach(u->notify(u.getId(),kind,title,body,type,id));
 }
 public void send(String title,String body) { toAdmins(NotificationKind.OPERATIONS,title,body,null,null); }
 public void toAdmins(String title,String body,Long id) { toAdmins(NotificationKind.ACCOUNT,title,body,RelatedType.USER,id); }
 public void toStudent(Long id,String title,String body) { notify(id,NotificationKind.ACCOUNT,title,body,RelatedType.USER,id); }
 public void toStudents(Long id,String title,String body) {
  users.findAllByRole(Role.STUDENT).forEach(u->notify(u.getId(),NotificationKind.NOTICE,title,body,RelatedType.NOTICE,id));
 }
 public void newRequest(String type,Long id,String title) {
  toAdmins(NotificationKind.REQUEST,title,title,RelatedType.valueOf(type),id);
 }
 public void answered(Long userId,String type,Long id,String title) {
  notify(userId,NotificationKind.RESPONSE,title,title,RelatedType.valueOf(type),id);
 }
 public void register(Long userId,String token,PushPlatform platform) {
  var saved=tokens.findByToken(token).orElseGet(()->PushToken.builder().token(token).build());
  saved.register(userId,platform,LocalDateTime.now(clock)); tokens.save(saved);
 }
 public void remove(Long userId,String token) { if(token!=null) tokens.deleteByUserIdAndToken(userId,token); }
 public void read(Long userId,Long id) {
  notifications.findByIdAndUserId(id,userId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND)).markRead();
 }
 public void readAll(Long userId) { notifications.markAllRead(userId); }
 public record NotificationCreated(NotificationResponse notification,Long userId) { }
}
