package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class RentalCancelService {
 private final RentalReader reader;
 private final RentalModelLockRepository locks;
 private final NotificationService notifications;
 private final AuditLogService audit;
 private final EntityManager em;
 private final Clock clock;
 public void cancel(Rental rental,String reason) {
  if(rental.getStatus()!=RentalStatus.RESERVED) return;
  rental.cancel(reason);
  notifications.notify(rental.getUserId(),NotificationKind.CANCEL,"예약 취소",reason,RelatedType.RENTAL,rental.getId());
 }
 public void cancel(Long id,Long actor,boolean admin,String reason) {
  var rental=reader.owned(id,actor,admin);
  locks.lockById(rental.getModelId()).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  em.refresh(rental);
  if(rental.getStatus()!=RentalStatus.RESERVED || (!admin && !rental.getStartAt().isAfter(LocalDateTime.now(clock))))
   throw new BusinessException(ErrorCode.NOT_CANCELLABLE);
  cancel(rental,reason);
  if(admin) audit.record("CANCEL","대여 "+id+": RESERVED -> CANCELLED, "+reason,actor);
 }
}
