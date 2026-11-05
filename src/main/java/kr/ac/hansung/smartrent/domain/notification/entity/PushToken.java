package kr.ac.hansung.smartrent.domain.notification.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Getter @Entity @Table(name="push_tokens") @Builder
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PRIVATE)
public class PushToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId;
 @Column(nullable=false,unique=true,length=512) private String token;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=10) private PushPlatform platform;
 @Column(nullable=false) private LocalDateTime updatedAt;
 public void register(Long userId, PushPlatform platform, LocalDateTime at) {
  this.userId=userId; this.platform=platform; this.updatedAt=at;
 }
}
