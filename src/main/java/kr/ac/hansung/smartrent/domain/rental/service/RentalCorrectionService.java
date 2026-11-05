package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.warning.entity.WarningKind;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class RentalCorrectionService {
 private final EntityManager em;
 private final RentalReader reader;
 private final RentalModelLockRepository locks;
 private final RentalPickupService pickup;
 private final OperationChangeService changes;
 private final WarningService warnings;
 private final AuditLogService audit;
 private final Clock clock;
 private Rental lock(Long id,RentalStatus expected) {
  var r=reader.require(id); locks.lockById(r.getModelId()).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  em.flush(); em.refresh(r);
  if(r.getStatus()!=expected) throw new BusinessException(ErrorCode.INVALID_STATUS);
  return r;
 }
 public RentalResponse noShow(Long id,String code,LocalDateTime actual,boolean checked,Long actor) {
  var r=lock(id,RentalStatus.NO_SHOW); var model=em.find(EquipmentModel.class,r.getModelId());
  if(!actual.isBefore(LocalDateTime.now(clock)) || !actual.isBefore(r.getEndAt())) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
  if(model.getRentalType()==RentalType.LONG_TERM && !checked) throw new BusinessException(ErrorCode.PROFESSOR_MAIL_REQUIRED);
  var unit=pickup.requireUnit(model,code);
  r.pickup(unit.getId(),actual,checked); warnings.revokeForCorrection(r,WarningKind.NO_SHOW);
  audit.record("NO_SHOW_CORRECTION","대여 "+id+": NO_SHOW -> RENTING, pickedAt="+actual,actor);
  return reader.response(r);
 }
 public RentalResponse returned(Long id,LocalDateTime actual,Long actor) {
  var r=lock(id,RentalStatus.RETURNED);
  if(actual.isBefore(r.getPickedAt()) || actual.isAfter(LocalDateTime.now(clock))) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
  var before=r.getReturnedAt(); r.correctReturnedAt(actual); warnings.correctLate(r);
  audit.record("RETURN_CORRECTION","대여 "+id+": returnedAt="+before+" -> "+actual,actor);
  return reader.response(r);
 }
 public RentalResponse damage(Long id,String note,Long actor) {
  var r=lock(id,RentalStatus.RETURNED); var now=LocalDateTime.now(clock);
  var unit=em.find(EquipmentUnit.class,r.getUnitId());
  var before=unit.getCondition(); unit.changeCondition(UnitCondition.DAMAGED); r.discoverDamage(now,note);
  if(!r.isBrokenReported()) warnings.give(r,WarningKind.DAMAGE,now);
  changes.capacityChanged(r.getModelId());
  audit.record("DAMAGE_DISCOVERY","대여 "+id+": 기기 "+unit.getId()+" "+before+" -> DAMAGED, damageDiscoveredAt="+now,actor);
  return reader.response(r);
 }
}
