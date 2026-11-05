package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.dto.AdminRentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RentalLookupService {
 private final EntityManager em;
 private final EquipmentUnitRepository units;
 private final RentalReader reader;
 private final Clock clock;
 public AdminRentalResponse response(Rental r) {
  var u=em.find(User.class,r.getUserId());
  return new AdminRentalResponse(reader.response(r),new AdminRentalResponse.Student(u.getId(),u.getName(),u.getStudentNumber()));
 }
 public List<AdminRentalResponse> pickups(String unitCode,String studentNumber) {
  if((unitCode==null || unitCode.isBlank()) && (studentNumber==null || studentNumber.isBlank()))
   throw new BusinessException(ErrorCode.VALIDATION_ERROR);
  Long model=unitCode==null?null:units.findByCodeAndDeletedAtIsNull(unitCode).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND)).getModelId();
  var date=LocalDate.now(clock);
  return em.createQuery("select r from Rental r join User u on u.id=r.userId where r.status=:status and r.startAt>=:start and r.startAt<:end and (:model is null or r.modelId=:model) and (:number is null or u.studentNumber=:number) order by r.startAt,r.id",Rental.class)
   .setParameter("status",RentalStatus.RESERVED).setParameter("start",date.atStartOfDay()).setParameter("end",date.plusDays(1).atStartOfDay())
   .setParameter("model",model).setParameter("number",studentNumber).getResultList().stream().map(this::response).toList();
 }
 public AdminRentalResponse returns(String code) {
  var unit=units.findByCodeAndDeletedAtIsNull(code).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  return em.createQuery("select r from Rental r where r.unitId=:id and r.status=:status",Rental.class)
   .setParameter("id",unit.getId()).setParameter("status",RentalStatus.RENTING).getResultList().stream().findFirst()
   .map(this::response).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
 }
}
