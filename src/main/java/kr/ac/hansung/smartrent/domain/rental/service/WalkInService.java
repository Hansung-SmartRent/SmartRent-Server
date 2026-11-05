package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.dto.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.*;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.dto.AdminStudentResponse;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.*;
import kr.ac.hansung.smartrent.global.security.StudentAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class WalkInService {
 private final EntityManager em;
 private final UserRepository users;
 private final RentalModelLockRepository locks;
 private final EquipmentUnitRepository units;
 private final RentalApplicationRepository applications;
 private final RentalRepository rentals;
 private final RentalReader reader;
 private final RentalPickupService pickup;
 private final ReservationService reservations;
 private final RentalEndOptions ends;
 private final RentalCalendarReader calendars;
 private final StudentAccessGuard guard;
 private final WarningService warnings;
 private final NotificationService notifications;
 private final AuditLogService audit;
 private final Clock clock;
 public record Options(AdminStudentResponse student,boolean eligible,String blockReason,List<RentalResponse.UnitBrief> units,List<LocalDateTime> endOptions) { }
 @Transactional(readOnly=true)
 public Options options(String number,Long modelId) {
  var student=users.findByStudentNumber(number).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  var model=em.find(EquipmentModel.class,modelId);
  if(model==null) throw new BusinessException(ErrorCode.NOT_FOUND);
  var now=LocalDateTime.now(clock);
  var info=AdminStudentResponse.of(student,warnings.count(student.getId()),now);
  var calendar=calendars.read(model);
  if(calendar.isClosed(now.toLocalDate())) return new Options(info,false,calendar.closedReason(now.toLocalDate()),List.of(),List.of());
  try {
   guard.check(student); if(model.isArchived()) throw new BusinessException(ErrorCode.MODEL_ARCHIVED);
   pickup.ensureOperating(model,now); reservations.checkStudent(student.getId(),model);
  } catch(BusinessException ex) { return new Options(info,false,ex.getMessage(),List.of(),List.of()); }
  var free=units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(modelId).stream()
   .filter(u->u.getCondition()==UnitCondition.NORMAL)
   .filter(u->em.createQuery("select count(r) from Rental r where r.unitId=:id and r.status=:status",Long.class)
    .setParameter("id",u.getId()).setParameter("status",RentalStatus.RENTING).getSingleResult()==0)
   .map(u->new RentalResponse.UnitBrief(u.getId(),u.getCode(),u.getName())).toList();
  var options=ends.options(model,now,now,null);
  boolean eligible=!free.isEmpty() && !options.isEmpty();
  return new Options(info,eligible,eligible?null:ErrorCode.NO_CAPACITY.getMessage(),free,options);
 }
 public RentalResponse walkIn(String number,Long modelId,String unitCode,LocalDateTime end,boolean checked,Long actor) {
  var student=users.findByStudentNumber(number).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  var model=locks.lockById(modelId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  em.refresh(student,LockModeType.PESSIMISTIC_WRITE);
  guard.check(student); if(model.isArchived()) throw new BusinessException(ErrorCode.MODEL_ARCHIVED);
  var now=LocalDateTime.now(clock); pickup.ensureOperating(model,now); reservations.checkStudent(student.getId(),model);
  if(model.getRentalType()==RentalType.LONG_TERM && !checked) throw new BusinessException(ErrorCode.PROFESSOR_MAIL_REQUIRED);
  var unit=pickup.requireUnit(model,unitCode);
  if(!ends.options(model,now,now,null).contains(end)) throw new BusinessException(ErrorCode.WALK_IN_END_INVALID);
  var app=applications.save(RentalApplication.builder().userId(student.getId()).modelId(modelId).source(ApplicationSource.WALK_IN).build());
  var r=rentals.save(Rental.builder().applicationId(app.getId()).userId(student.getId()).modelId(modelId).unitId(unit.getId())
   .status(RentalStatus.RENTING).startAt(now).pickedAt(now).endAt(end).professorMailChecked(checked).build());
  notifications.notify(student.getId(),NotificationKind.PICKUP,"대여 시작","현장 대여가 처리되었습니다.",RelatedType.RENTAL,r.getId());
  audit.record("WALK_IN","대여 "+r.getId()+": 현장 대여, 기기 "+unit.getId(),actor);
  return reader.response(r);
 }
}
