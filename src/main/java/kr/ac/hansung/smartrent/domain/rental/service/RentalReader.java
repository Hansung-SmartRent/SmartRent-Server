package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse.UnitBrief;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RentalReader {
 private final EntityManager em;
 private final Clock clock;
 public Rental require(Long id) {
  var rental=em.find(Rental.class,id); if(rental==null) throw new BusinessException(ErrorCode.NOT_FOUND); return rental;
 }
 public Rental owned(Long id,Long userId,boolean admin) {
  var r=require(id);
  if(!admin && !r.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
  return r;
 }
 public RentalResponse response(Rental r) {
  var model=em.find(EquipmentModel.class,r.getModelId());
  var app=em.find(RentalApplication.class,r.getApplicationId());
  var unit=r.getUnitId()==null ? null : em.find(EquipmentUnit.class,r.getUnitId());
  var user=em.find(User.class,r.getUserId());
  var now=LocalDateTime.now(clock);
  return new RentalResponse(r.getId(),r.getApplicationId(),app.getSource(),model.getId(),model.getName(),model.getRentalType(),
   unit==null ? null : new UnitBrief(unit.getId(),unit.getCode(),unit.getName()),
   r.getStatus(),r.getStartAt(),r.getEndAt(),r.getPickedAt(),r.getReturnedAt(),r.isExtended(),r.isBrokenReported(),
   r.isReturnRequired(),r.getCancelReason(),r.getStatus()==RentalStatus.RESERVED && r.getStartAt().isAfter(now),
   r.getStatus()==RentalStatus.RENTING && model.getRentalType()==RentalType.GENERAL && !r.isExtended()
    && r.getEndAt().isAfter(now) && !r.isReturnRequired() && !user.isSuspended(now),
   r.getBrokenNote(),r.getReturnCondition(),r.getDamageDiscoveredAt());
 }
 public List<RentalResponse> mine(Long id,String group) {
  List<RentalStatus> statuses=switch(group==null ? "ALL" : group) {
   case "ALL" -> List.of(RentalStatus.values());
   case "RESERVED" -> List.of(RentalStatus.RESERVED);
   case "RENTING" -> List.of(RentalStatus.RENTING);
   case "DONE" -> List.of(RentalStatus.RETURNED,RentalStatus.CANCELLED,RentalStatus.NO_SHOW);
   default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
  };
  return em.createQuery("select r from Rental r join RentalApplication a on a.id=r.applicationId where r.userId=:id and r.status in :statuses order by a.createdAt desc,a.id desc,r.startAt,r.id",Rental.class)
   .setParameter("id",id).setParameter("statuses",statuses).getResultList().stream().map(this::response).toList();
 }
}
