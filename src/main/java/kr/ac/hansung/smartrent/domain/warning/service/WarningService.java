package kr.ac.hansung.smartrent.domain.warning.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.WarningRepository;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCancelService;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.dto.MeResponse.Suspension;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.*;
import kr.ac.hansung.smartrent.global.port.ActiveWarningCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class WarningService implements ActiveWarningCounter {
 private final WarningRepository warnings;
 private final EntityManager em;
 private final RentalCancelService cancel;
 private final NotificationService notifications;
 private final AuditLogService audit;
 private final Clock clock;
 @Transactional(readOnly=true)
 public long count(Long userId) {
  var now=LocalDateTime.now(clock);
  return warnings.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().filter(w->w.active(now)).count();
 }
 public Warning give(Rental rental,WarningKind kind,LocalDateTime at) {
  var user=em.find(User.class,rental.getUserId(),LockModeType.PESSIMISTIC_WRITE);
  var old=warnings.findByRentalIdAndKind(rental.getId(),kind);
  if(old.isPresent()) return old.get();
  if(kind==WarningKind.NO_SHOW) {
   var same=em.createQuery("select w from Warning w join Rental r on r.id=w.rentalId where r.applicationId=:app and w.kind=:kind",Warning.class)
    .setParameter("app",rental.getApplicationId()).setParameter("kind",kind).getResultList();
   if(!same.isEmpty()) return same.getFirst();
  }
  var result=warnings.save(Warning.builder().userId(user.getId()).rentalId(rental.getId()).kind(kind).createdAt(at).build());
  notifications.notify(user.getId(),NotificationKind.WARNING,"경고 안내",kind.name()+" 경고가 부여되었습니다.",RelatedType.WARNING,result.getId());
  reconcile(user,at);
  return result;
 }
 public void reconcile(User user,LocalDateTime at) {
  var held=held(user.getId());
  if(count(user.getId())<3) {
   user.changeSuspension(null,false,false); held.forEach(r->r.requireReturn(false)); return;
  }
  if(user.isSuspended(at)) return;
  if(held.isEmpty()) user.changeSuspension(at.plusMonths(6),false,false);
  else {
   user.changeSuspension(null,true,false);
   for(var r:held) {
    r.requireReturn(true);
    notifications.notify(user.getId(),NotificationKind.RETURN_REQUIRED,"즉시 반납 요망","보유 중인 기자재를 반납해 주세요.",RelatedType.RENTAL,r.getId());
   }
  }
  em.createQuery("select r from Rental r where r.userId=:id and r.status=:status order by r.modelId,r.id",Rental.class)
   .setParameter("id",user.getId()).setParameter("status",RentalStatus.RESERVED).getResultList()
   .forEach(r->cancel.cancel(r,"경고 3회 이용 정지"));
  notifications.notify(user.getId(),NotificationKind.SUSPEND,"이용 정지","경고 3회로 이용이 정지되었습니다.",RelatedType.USER,user.getId());
 }
 public void revoke(Long id,String reason,Long actor) {
  var w=warnings.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  var user=em.find(User.class,w.getUserId(),LockModeType.PESSIMISTIC_WRITE);
  if(!w.active(LocalDateTime.now(clock))) throw new BusinessException(ErrorCode.WARNING_NOT_ACTIVE);
  w.revoke(LocalDateTime.now(clock),reason);
  reconcile(user,LocalDateTime.now(clock));
  notifications.notify(user.getId(),NotificationKind.WARNING,"경고 취소",reason,RelatedType.WARNING,id);
  audit.record("WARNING_REVOKE","경고 "+id+": 유효 -> 취소, "+reason,actor);
 }
 public void revokeForCorrection(Rental r,WarningKind kind) {
  warnings.findByRentalIdAndKind(r.getId(),kind).filter(w->w.getRevokedAt()==null).ifPresent(w->w.revoke(LocalDateTime.now(clock),"관리자 시각 정정"));
  reconcile(em.find(User.class,r.getUserId(),LockModeType.PESSIMISTIC_WRITE),LocalDateTime.now(clock));
 }
 public void correctLate(Rental r) {
  if(!r.getReturnedAt().isAfter(r.getEndAt())) revokeForCorrection(r,WarningKind.LATE);
  else {
   var previous=warnings.findByRentalIdAndKind(r.getId(),WarningKind.LATE);
   if(previous.isPresent() && "관리자 시각 정정".equals(previous.get().getRevokeReason())) {
    previous.get().reinstate(); reconcile(em.find(User.class,r.getUserId(),LockModeType.PESSIMISTIC_WRITE),r.getReturnedAt());
   } else if(previous.isEmpty()) give(r,WarningKind.LATE,r.getEndAt());
  }
  var user=em.find(User.class,r.getUserId());
  if(user.isSuspensionStartedByReturn() && count(user.getId())>=3 && held(user.getId()).isEmpty()) {
   var last=em.createQuery("select max(r.returnedAt) from Rental r where r.userId=:id",LocalDateTime.class)
    .setParameter("id",user.getId()).getSingleResult();
   user.changeSuspension(last.plusMonths(6),false,true);
  }
 }
 public void returned(Long userId,LocalDateTime at) {
  var user=em.find(User.class,userId,LockModeType.PESSIMISTIC_WRITE);
  if(user.isSuspendedUntilReturn() && held(userId).isEmpty()) user.changeSuspension(at.plusMonths(6),false,true);
 }
 public void expireSuspensions() {
  var now=LocalDateTime.now(clock);
  for(var user:em.createQuery("select u from User u where u.suspendedUntil<=:now",User.class).setParameter("now",now).getResultList()) {
   em.lock(user,LockModeType.PESSIMISTIC_WRITE);
   var until=user.getSuspendedUntil();
   warnings.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream().filter(w->w.active(until.minusNanos(1))).forEach(w->w.expire(until));
   user.changeSuspension(null,false,false);
  }
 }
 private List<Rental> held(Long userId) {
  return em.createQuery("select r from Rental r where r.userId=:id and r.status=:status",Rental.class)
   .setParameter("id",userId).setParameter("status",RentalStatus.RENTING).getResultList();
 }
 @Transactional(readOnly=true) public List<Item> items(Long userId) {
  return warnings.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(w->{
   Rental r=w.getRentalId()==null?null:em.find(Rental.class,w.getRentalId());
   String name=r==null?null:em.find(EquipmentModel.class,r.getModelId()).getName();
   return new Item(w.getId(),w.getKind(),w.getRentalId(),name,w.getCreatedAt(),w.getRevokedAt(),w.getExpiresAt(),w.getRevokeReason());
  }).toList();
 }
 @Transactional(readOnly=true) public MyWarnings mine(Long id) {
  var u=em.find(User.class,id); return new MyWarnings(count(id),new Suspension(u.isSuspended(LocalDateTime.now(clock)),u.getSuspendedUntil(),u.isSuspendedUntilReturn()),items(id));
 }
 public record Item(Long id,WarningKind kind,Long rentalId,String modelName,LocalDateTime createdAt,LocalDateTime revokedAt,LocalDateTime expiresAt,String revokeReason) { }
 public record MyWarnings(long activeCount,Suspension suspension,List<Item> items) { }
}
