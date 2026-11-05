package kr.ac.hansung.smartrent.domain.notification.entity;
import jakarta.persistence.*;
import lombok.*;
@Getter @Entity @Table(name="notification_settings") @Builder
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PRIVATE)
public class NotificationSettings {
 @Id private Long userId;
 @Column(nullable=false) private boolean dueEnabled;
 public void change(boolean enabled) { dueEnabled=enabled; }
}
