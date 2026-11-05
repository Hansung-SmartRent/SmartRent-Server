package kr.ac.hansung.smartrent.domain.warning.service;
import java.time.*;
import jakarta.persistence.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import kr.ac.hansung.smartrent.domain.warning.repository.*;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCancelService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.port.RetainedWarningRestorer;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class RetainedWarningService implements RetainedWarningRestorer {
 private final EntityManager em;
 private final WarningRepository warnings;
 private final RetainedWarningRepository retained;
 private final RentalCancelService cancel;
 private final AuditLogService audit;
 private final Clock clock;
 public void retain(Long userId) {
  var user=em.find(User.class,userId,LockModeType.PESSIMISTIC_WRITE);
  if(user==null) throw new BusinessException(ErrorCode.NOT_FOUND);
  if(em.createQuery("select count(r) from Rental r where r.userId=:id and r.status=:status",Long.class)
   .setParameter("id",userId).setParameter("status",RentalStatus.RENTING).getSingleResult()>0)
   throw new BusinessException(ErrorCode.WITHDRAW_BLOCKED_HELD);
  var now=LocalDateTime.now(clock);
  var active=warnings.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().filter(w->w.active(now)).toList();
  for(var w:active) retained.save(RetainedWarning.builder().studentNumber(user.getStudentNumber()).kind(w.getKind())
   .createdAt(w.getCreatedAt()).revokedAt(w.getRevokedAt()).expiresAt(w.getExpiresAt())
   .originalSuspendedUntil(user.getSuspendedUntil()).retainUntil(now.plusMonths(6)).build());
  warnings.deleteAll(warnings.findByUserIdOrderByCreatedAtDescIdDesc(userId));
  em.createQuery("select r from Rental r where r.userId=:id and r.status=:status",Rental.class)
   .setParameter("id",userId).setParameter("status",RentalStatus.RESERVED).getResultList().forEach(r->cancel.cancel(r,"회원 탈퇴"));
  audit.record("WITHDRAW",active.isEmpty() ? "탈퇴 회원" : "탈퇴 학번 "+user.getStudentNumber(),null,
   active.isEmpty()?null:now.plusMonths(6));
 }
 public void restore(Long userId,String studentNumber) {
  var user=em.find(User.class,userId,LockModeType.PESSIMISTIC_WRITE);
  var now=LocalDateTime.now(clock);
  LocalDateTime suspension=null;
  for(var w:retained.findByStudentNumber(studentNumber)) {
   if(w.getRetainUntil().isAfter(now)) {
    var original=w.getOriginalSuspendedUntil();
    var expired=original!=null && !original.isAfter(now) ? original : w.getExpiresAt();
    warnings.save(Warning.builder().userId(userId).kind(w.getKind()).createdAt(w.getCreatedAt())
     .revokedAt(w.getRevokedAt()).expiresAt(expired).build());
    if(original!=null && original.isAfter(now) && (suspension==null || original.isAfter(suspension))) suspension=original;
   }
   retained.delete(w);
  }
  if(suspension!=null) user.changeSuspension(suspension,false,false);
 }
 public void purgeExpired() {
  var now=LocalDateTime.now(clock);
  em.createQuery("delete from RetainedWarning w where w.retainUntil<=:now").setParameter("now",now).executeUpdate();
  em.createQuery("update AuditLog a set a.detail='탈퇴 회원',a.retainUntil=null where a.action='WITHDRAW' and a.retainUntil<=:now")
   .setParameter("now",now).executeUpdate();
 }
}
