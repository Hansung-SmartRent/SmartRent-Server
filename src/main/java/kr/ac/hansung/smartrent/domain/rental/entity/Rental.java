package kr.ac.hansung.smartrent.domain.rental.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kr.ac.hansung.smartrent.global.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "rentals")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rental extends BaseTimeEntity {
 public void cancel(String reason) { status=RentalStatus.CANCELLED; cancelReason=reason; }
 public void noShow() { status=RentalStatus.NO_SHOW; cancelReason="미수령"; }
 public void pickup(Long unitId,LocalDateTime at,boolean checked) {
  this.unitId=unitId; pickedAt=at; professorMailChecked=checked; status=RentalStatus.RENTING;
 }
 public void returnAt(LocalDateTime at,ReturnCondition condition) {
  returnedAt=at; returnCondition=condition; status=RentalStatus.RETURNED;
 }
 public void correctReturnedAt(LocalDateTime at) { returnedAt=at; }
 public void extend(LocalDateTime end) { endAt=end; extended=true; dueNotified=false; }
 public void changeDeadline(LocalDateTime end) { endAt=end; dueNotified=false; }
 public void requireReturn(boolean required) { returnRequired=required; }
 public void markDueNotified() { dueNotified=true; }
 public void markLateWarned() { lateWarned=true; }
 public void discoverDamage(LocalDateTime at,String note) { damageDiscoveredAt=at; damageNote=note; }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "model_id", nullable = false)
	private Long modelId;

	@Column(name = "unit_id", nullable = true)
	private Long unitId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 12)
	private RentalStatus status;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private LocalDateTime endAt;

	@Column(name = "picked_at", nullable = true)
	private LocalDateTime pickedAt;

	@Column(name = "returned_at", nullable = true)
	private LocalDateTime returnedAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "return_condition", nullable = true, length = 10)
	private ReturnCondition returnCondition;

	@Column(name = "extended", nullable = false)
	private boolean extended;

	@Column(name = "broken_reported", nullable = false)
	private boolean brokenReported;

	@Column(name = "broken_note", nullable = true, length = 500)
	private String brokenNote;

	@Column(name = "damage_discovered_at", nullable = true)
	private LocalDateTime damageDiscoveredAt;

	@Column(name = "damage_note", nullable = true, length = 500)
	private String damageNote;

	@Column(name = "return_required", nullable = false)
	private boolean returnRequired;

	@Column(name = "professor_mail_checked", nullable = false)
	private boolean professorMailChecked;

	@Column(name = "cancel_reason", nullable = true, length = 100)
	private String cancelReason;

	@Column(name = "due_notified", nullable = false)
	private boolean dueNotified;

	@Column(name = "late_warned", nullable = false)
	private boolean lateWarned;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;
}
