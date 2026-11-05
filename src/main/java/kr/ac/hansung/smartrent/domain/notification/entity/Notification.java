package kr.ac.hansung.smartrent.domain.notification.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Getter @Entity @Table(name="notifications") @Builder
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PRIVATE)
public class Notification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=20) private NotificationKind kind;
 @Column(nullable=false,length=100) private String title;
 @Column(nullable=false,length=1000) private String body;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length=20) private RelatedType relatedType;
 private Long relatedId;
 @Column(name="is_read",nullable=false) private boolean read;
 @Column(nullable=false) private LocalDateTime createdAt;
 public void markRead() { read=true; }
}
