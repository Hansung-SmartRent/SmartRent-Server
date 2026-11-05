package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.global.exception.*;
import kr.ac.hansung.smartrent.global.security.StudentAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class RentalExtensionService {
 private final EntityManager em;
 private final RentalReader reader;
 private final RentalModelLockRepository locks;
 private final RentalEndOptions ends;
 private final StudentAccessGuard guard;
 private final Clock clock;
 public record Options(boolean extendable,String reason,LocalDateTime maxEnd,List<LocalDateTime> options) { }
 @Transactional(readOnly=true)
 public Options options(Long id,Long user) {
  var r=reader.owned(id,user,false); var model=em.find(EquipmentModel.class,r.getModelId());
  var error=blocked(r,model);
  if(error!=null) return new Options(false,error.name(),null,List.of());
  var options=ends.options(model,r.getStartAt(),r.getEndAt(),id);
  return new Options(!options.isEmpty(),options.isEmpty()?ErrorCode.EXTENSION_LIMIT.name():null,
   options.isEmpty()?null:options.getLast(),options);
 }
 public RentalResponse extend(Long id,Long user,LocalDateTime newEnd) {
  var r=reader.owned(id,user,false);
  var model=locks.lockById(r.getModelId()).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND)); em.flush(); em.refresh(r);
  guard.check(user); var error=blocked(r,model); if(error!=null) throw new BusinessException(error);
  if(!ends.options(model,r.getStartAt(),r.getEndAt(),id).contains(newEnd)) throw new BusinessException(ErrorCode.EXTENSION_LIMIT);
  r.extend(newEnd); return reader.response(r);
 }
 private ErrorCode blocked(Rental r,EquipmentModel model) {
  if(model.getRentalType()==RentalType.LONG_TERM) return ErrorCode.LONG_TERM_EXTENSION_BLOCKED;
  if(r.getStatus()!=RentalStatus.RENTING || r.isExtended() || r.isReturnRequired() || !r.getEndAt().isAfter(LocalDateTime.now(clock)))
   return ErrorCode.EXTENSION_NOT_ALLOWED;
  return null;
 }
}
