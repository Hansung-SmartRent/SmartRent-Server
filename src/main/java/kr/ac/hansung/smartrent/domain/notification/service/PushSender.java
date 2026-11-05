package kr.ac.hansung.smartrent.domain.notification.service;
import kr.ac.hansung.smartrent.domain.notification.dto.NotificationResponse;
public interface PushSender {
 enum Result { SENT, UNREGISTERED, FAILED }
 Result send(String token, NotificationResponse notification);
}
