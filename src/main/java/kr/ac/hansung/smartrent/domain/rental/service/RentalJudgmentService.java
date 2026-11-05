package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.warning.entity.WarningKind;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class RentalJudgmentService {
 private final EntityManager em;
 private final RentalModelLockRepository locks;
 private final EquipmentUnitRepository units;
 private final RentalCalendarReader calendars;
 private final RentalCancelService cancel;
 private final WarningService warnings;
 private final NotificationService notifications;
 private final Clock clock;
 public void processModel(Long id) {
  var model=locks.lockById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  var now=LocalDateTime.now(clock);
  var all=em.createQuery("select r from Rental r where r.modelId=:id order by r.startAt,r.id",Rental.class)
   .setParameter("id",id).getResultList();
  var normal=units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(id).stream().filter(u->u.getCondition()==UnitCondition.NORMAL).toList();
  var changes=new LinkedHashMap<Long,LocalDateTime>();
  for(var r:all) {
   if(r.getStatus()!=RentalStatus.RESERVED || r.getStartAt().isAfter(now)) continue;
   long occupied=all.stream().filter(other->!other.getId().equals(r.getId()) && other.getUnitId()!=null &&
    normal.stream().anyMatch(u->u.getId().equals(other.getUnitId())) && other.getPickedAt()!=null &&
    !other.getPickedAt().isAfter(r.getStartAt()) &&
    (other.getReturnedAt()==null || other.getReturnedAt().isAfter(r.getStartAt())))
    .map(Rental::getUnitId).distinct().count();
   if(occupied>=normal.size()) {
    cancel.cancel(r,"예약 시작 시 기기 부족"); changes.put(r.getId(),r.getStartAt()); continue;
   }
   var deadline=noShowDeadline(r,model);
   if(!deadline.isAfter(now)) {
    r.noShow(); changes.put(r.getId(),deadline);
    var linked=em.createQuery("select x from Rental x where x.applicationId=:id and x.status=:status",Rental.class)
     .setParameter("id",r.getApplicationId()).setParameter("status",RentalStatus.RESERVED).getResultList();
    for(var x:linked) { cancel.cancel(x,"연결 신청 미수령"); changes.put(x.getId(),deadline); }
    warnings.give(r,WarningKind.NO_SHOW,deadline);
   }
  }
  for(var r:all) {
   if(r.getStatus()!=RentalStatus.RENTING) continue;
   if(r.getEndAt().isBefore(now) && !r.isLateWarned()) {
    r.markLateWarned(); warnings.give(r,WarningKind.LATE,r.getEndAt());
   }
   var dueAt=model.getRentalType()==RentalType.LONG_TERM ? r.getEndAt().minusDays(7)
    : model.isMultiDay()?r.getEndAt().minusDays(1):r.getEndAt().minusMinutes(15);
   if(!r.isDueNotified() && !dueAt.isAfter(now) && now.isBefore(r.getEndAt())) {
    r.markDueNotified();
    notifications.notify(r.getUserId(),NotificationKind.DUE,"반납 기한 안내","반납 기한을 확인해 주세요.",RelatedType.RENTAL,r.getId());
   }
  }
  em.flush();
  changes.forEach((rentalId,at)->em.createNativeQuery("update rentals set updated_at=:at where id=:id")
   .setParameter("at",at).setParameter("id",rentalId).executeUpdate());
 }
 public LocalDateTime noShowDeadline(Rental r,EquipmentModel model) {
  return model.getRentalType()==RentalType.LONG_TERM
   ? r.getStartAt().toLocalDate().atTime(calendars.read(model).hours(r.getStartAt().toLocalDate()).close())
   : r.getStartAt().plusMinutes(10);
 }
}
