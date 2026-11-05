package kr.ac.hansung.smartrent.domain.notification.dto;
import java.time.LocalDateTime;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
public record NotificationResponse(Long id, NotificationKind kind, String title, String body,
 RelatedType relatedType, Long relatedId, boolean read, LocalDateTime createdAt) {
 public static NotificationResponse of(Notification n) {
  return new NotificationResponse(n.getId(), n.getKind(),n.getTitle(),n.getBody(),
   n.getRelatedType(),n.getRelatedId(),n.isRead(),n.getCreatedAt());
 }
}
