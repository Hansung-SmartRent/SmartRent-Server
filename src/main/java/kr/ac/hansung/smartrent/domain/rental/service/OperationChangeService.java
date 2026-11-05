package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.*;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 호출자는 모델 잠금을 잡고 운영·기기 상태를 저장한 뒤 같은 트랜잭션에서 호출합니다(DB 6절). */
@Service @RequiredArgsConstructor @Transactional
public class OperationChangeService {
 private final RentalModelLockRepository locks;
 private final EquipmentUnitRepository units;
 private final EntityManager em;
 private final RentalCalendarReader calendars;
 private final RentalCancelService cancel;
 private final RentalReader reader;
 private final NotificationService notifications;
 private final Clock clock;
 public record Result(List<RentalResponse> cancelled,List<RentalResponse> extended) { }
 private EquipmentModel lock(Long id) {
  return locks.lockById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
 }
 private List<Rental> reserved(Long id) {
  return em.createQuery("select r from Rental r join RentalApplication a on a.id=r.applicationId where r.modelId=:id and r.status=:status order by a.createdAt,a.id,r.startAt,r.id",Rental.class)
   .setParameter("id",id).setParameter("status",RentalStatus.RESERVED).getResultList();
 }
 public Result capacityChanged(Long modelId) {
  var model=lock(modelId); em.flush();
  var availableUnits=units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(modelId);
  var kept=new ArrayList<>(em.createQuery("select r from Rental r where r.modelId=:id and r.status=:status",Rental.class)
   .setParameter("id",modelId).setParameter("status",RentalStatus.RENTING).getResultList());
  var cancelled=new ArrayList<Rental>();
  var calculator=new RentalCapacityCalculator(clock);
  for(var r:reserved(modelId)) {
   if(calculator.available(model.isCarryOut(),availableUnits,kept,new RentalSegment(r.getStartAt(),r.getEndAt()))<1) {
    cancel.cancel(r,"기기 수량 변경"); cancelled.add(r);
   } else kept.add(r);
  }
  return result(cancelled,List.of());
 }
 public Result holidayAdded(LocalDate date) {
  var ids=em.createQuery("select m.id from EquipmentModel m order by m.id",Long.class).getResultList();
  var cancelled=new ArrayList<Rental>(); var extended=new ArrayList<Rental>();
  var now=LocalDateTime.now(clock);
  for(var id:ids) {
   var model=lock(id); var calendar=calendars.read(model);
   if(!calendar.isClosed(date)) continue;
   for(var r:reserved(id)) if(r.getStartAt().isBefore(date.plusDays(1).atStartOfDay()) && r.getEndAt().isAfter(date.atStartOfDay())) {
    cancel.cancel(r,"휴무일 지정"); cancelled.add(r);
   }
   for(var r:em.createQuery("select r from Rental r where r.modelId=:id and r.status=:status",Rental.class)
    .setParameter("id",id).setParameter("status",RentalStatus.RENTING).getResultList()) {
    if(r.getEndAt().toLocalDate().equals(date) && !r.getEndAt().isBefore(now) && !r.getEndAt().isAfter(now.plusDays(7))) {
     var next=date.plusDays(1);
     while(calendar.isClosed(next)) next=next.plusDays(1);
     r.changeDeadline(next.atTime(calendar.hours(next).close())); extended.add(r);
     notifications.notify(r.getUserId(),NotificationKind.BOOKING,"반납 기한 변경","휴무일 지정으로 반납 기한이 변경되었습니다.",RelatedType.RENTAL,r.getId());
    }
   }
  }
  return result(cancelled,extended);
 }
 public Result hoursChanged(Long modelId,LocalDate date) {
  var model=lock(modelId); var calendar=calendars.read(model);
  var cancelled=new ArrayList<Rental>();
  for(var r:reserved(modelId)) {
   boolean invalid=false;
   for(var day=r.getStartAt().toLocalDate();!day.isAfter(r.getEndAt().toLocalDate());day=day.plusDays(1)) {
    if(date!=null && !date.equals(day)) continue;
    var hours=calendar.hours(day);
    if(r.getStartAt().toLocalDate().equals(day) && r.getStartAt().toLocalTime().isBefore(hours.open())) invalid=true;
    if(r.getEndAt().toLocalDate().equals(day) && r.getEndAt().toLocalTime().isAfter(hours.close())) invalid=true;
   }
   if(invalid) { cancel.cancel(r,"운영 시간 변경"); cancelled.add(r); }
  }
  return result(cancelled,List.of());
 }
 public Result modelArchived(Long modelId) {
  lock(modelId);
  if(em.createQuery("select count(r) from Rental r where r.modelId=:id and r.status=:status",Long.class)
   .setParameter("id",modelId).setParameter("status",RentalStatus.RENTING).getSingleResult()>0)
   throw new BusinessException(ErrorCode.MODEL_ARCHIVE_BLOCKED);
  var cancelled=reserved(modelId); cancelled.forEach(r->cancel.cancel(r,"모델 운영 종료"));
  return result(cancelled,List.of());
 }
 private Result result(List<Rental> cancelled,List<Rental> extended) {
  return new Result(cancelled.stream().map(reader::response).toList(),extended.stream().map(reader::response).toList());
 }
}
