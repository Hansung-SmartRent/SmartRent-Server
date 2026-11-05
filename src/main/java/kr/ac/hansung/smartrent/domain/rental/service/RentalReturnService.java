package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.warning.entity.WarningKind;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class RentalReturnService {
 private final EntityManager em;
 private final RentalReader reader;
 private final RentalModelLockRepository locks;
 private final RentalJudgmentService judgments;
 private final OperationChangeService changes;
 private final EquipmentUnitRepository units;
 private final WarningService warnings;
 private final NotificationService notifications;
 private final AuditLogService audit;
 private final Clock clock;
 public RentalResponse returnRental(Long id,String code,ReturnCondition condition,Long actor) {
  var r=reader.require(id);
  locks.lockById(r.getModelId()).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND)); em.flush(); em.refresh(r);
  if(r.getStatus()!=RentalStatus.RENTING) throw new BusinessException(ErrorCode.INVALID_STATUS);
  var unit=units.findByCodeAndDeletedAtIsNull(code).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  if(!unit.getId().equals(r.getUnitId())) throw new BusinessException(ErrorCode.UNIT_MISMATCH);
  judgments.processModel(r.getModelId());
  var now=LocalDateTime.now(clock);
  r.returnAt(now,condition);
  if(now.isAfter(r.getEndAt())) { r.markLateWarned(); warnings.give(r,WarningKind.LATE,r.getEndAt()); }
  if(condition==ReturnCondition.DAMAGED && !r.isBrokenReported()) warnings.give(r,WarningKind.DAMAGE,now);
  unit.changeCondition(condition==ReturnCondition.DAMAGED?UnitCondition.DAMAGED:UnitCondition.NORMAL);
  if(condition==ReturnCondition.DAMAGED) changes.capacityChanged(r.getModelId());
  warnings.returned(r.getUserId(),now);
  notifications.notify(r.getUserId(),NotificationKind.RETURN,"반납 완료","반납이 처리되었습니다.",RelatedType.RENTAL,id);
  audit.record("RETURN","대여 "+id+": RENTING -> RETURNED, "+condition+", returnedAt="+now,actor);
  return reader.response(r);
 }
}
