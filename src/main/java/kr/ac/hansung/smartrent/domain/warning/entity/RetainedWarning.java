package kr.ac.hansung.smartrent.domain.warning.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Getter @Entity @Table(name="retained_warnings") @Builder
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PRIVATE)
public class RetainedWarning {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=20) private String studentNumber;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=10) private WarningKind kind;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime revokedAt;
 private LocalDateTime expiresAt;
 private LocalDateTime originalSuspendedUntil;
 @Column(nullable=false) private LocalDateTime retainUntil;
}
