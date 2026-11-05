package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import jakarta.persistence.*;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.*;
import kr.ac.hansung.smartrent.global.security.StudentAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class RentalPickupService {
 private final EntityManager em;
 private final RentalReader reader;
 private final RentalModelLockRepository locks;
 private final RentalJudgmentService judgments;
 private final RentalCalendarReader calendars;
 private final EquipmentUnitRepository units;
 private final StudentAccessGuard guard;
 private final NotificationService notifications;
 private final AuditLogService audit;
 private final Clock clock;
 public static class JudgmentRefusal extends BusinessException {
  public JudgmentRefusal(ErrorCode code) { super(code); }
 }
 @Transactional(noRollbackFor=JudgmentRefusal.class)
 public RentalResponse pickup(Long id,String code,boolean checked,Long actor) {
  var r=reader.require(id);
  var model=locks.lockById(r.getModelId()).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  em.flush(); em.refresh(r);
  boolean reserved=r.getStatus()==RentalStatus.RESERVED;
  judgments.processModel(model.getId());
  if(reserved && r.getStatus()==RentalStatus.NO_SHOW) throw new JudgmentRefusal(ErrorCode.NO_SHOW_DEADLINE_PASSED);
  if(reserved && r.getStatus()==RentalStatus.CANCELLED) throw new JudgmentRefusal(ErrorCode.INVALID_STATUS);
  if(r.getStatus()!=RentalStatus.RESERVED) throw new BusinessException(ErrorCode.INVALID_STATUS);
  var now=LocalDateTime.now(clock);
  guard.check(r.getUserId());
  ensureOperating(model,now);
  var unit=requireUnit(model,code);
  if(model.getRentalType()==RentalType.LONG_TERM && !checked) throw new BusinessException(ErrorCode.PROFESSOR_MAIL_REQUIRED);
  if(now.toLocalDate().isBefore(r.getStartAt().toLocalDate())) throw new BusinessException(ErrorCode.OUTSIDE_OPERATING_HOURS);
  if(now.isBefore(r.getStartAt())) {
   var available=new RentalCapacityCalculator(clock).available(model.isCarryOut(),
    units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(model.getId()),
    em.createQuery("select x from Rental x where x.modelId=:model and x.id<>:id and x.status in :statuses",Rental.class)
     .setParameter("model",model.getId()).setParameter("id",id).setParameter("statuses",java.util.List.of(RentalStatus.RESERVED,RentalStatus.RENTING)).getResultList(),
    new RentalSegment(now,r.getEndAt()));
   if(available<1) throw new BusinessException(ErrorCode.NO_EARLY_CAPACITY);
  }
  r.pickup(unit.getId(),now,checked);
  notifications.notify(r.getUserId(),NotificationKind.PICKUP,"대여 시작","기자재 수령이 처리되었습니다.",RelatedType.RENTAL,id);
  audit.record("PICKUP","대여 "+id+": RESERVED -> RENTING, 기기 "+unit.getId(),actor);
  return reader.response(r);
 }
 public void ensureOperating(EquipmentModel model,LocalDateTime at) {
  var calendar=calendars.read(model); var hours=calendar.hours(at.toLocalDate());
  if(calendar.isClosed(at.toLocalDate()) || at.toLocalTime().isBefore(hours.open()) || !at.toLocalTime().isBefore(hours.close()))
   throw new BusinessException(ErrorCode.OUTSIDE_OPERATING_HOURS);
 }
 public EquipmentUnit requireUnit(EquipmentModel model,String code) {
  var unit=units.findByCodeAndDeletedAtIsNull(code).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  if(!unit.getModelId().equals(model.getId()) || unit.getCondition()!=UnitCondition.NORMAL) throw new BusinessException(ErrorCode.UNIT_MISMATCH);
  if(em.createQuery("select count(r) from Rental r where r.unitId=:id and r.status=:status",Long.class)
   .setParameter("id",unit.getId()).setParameter("status",RentalStatus.RENTING).getSingleResult()>0) throw new BusinessException(ErrorCode.UNIT_IN_USE);
  return unit;
 }
}
