package kr.ac.hansung.smartrent.domain.warning.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Getter @Entity @Table(name="warnings") @Builder
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PRIVATE)
public class Warning {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=10) private WarningKind kind;
 private Long rentalId;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime revokedAt;
 @Column(length=100) private String revokeReason;
 @Column(nullable=false) private boolean revokedByCorrection;
 private LocalDateTime expiresAt;
 public boolean active(LocalDateTime now) { return revokedAt==null && (expiresAt==null || expiresAt.isAfter(now)); }
 public void revoke(LocalDateTime at,String reason) { revokedAt=at; revokeReason=reason; revokedByCorrection=false; }
 public void revokeForCorrection(LocalDateTime at) { revoke(at,"관리자 시각 정정"); revokedByCorrection=true; }
 public void expire(LocalDateTime at) { expiresAt=at; }
 public void reinstate() { revokedAt=null; revokeReason=null; revokedByCorrection=false; }
}
