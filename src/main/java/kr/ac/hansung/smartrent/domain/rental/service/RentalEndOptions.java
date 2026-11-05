package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RentalEndOptions {
 private final EntityManager em;
 private final RentalCalendarReader calendars;
 private final EquipmentUnitRepository units;
 private final Clock clock;
 public List<LocalDateTime> options(EquipmentModel model,LocalDateTime start,LocalDateTime after,Long excludeId) {
  var calendar=calendars.read(model); var periods=new RentalPeriodCalculator(clock);
  List<LocalDateTime> candidates;
  if(model.getRentalType()==RentalType.LONG_TERM) candidates=List.of(periods.longTermDeadline(start.toLocalDate(),calendar));
  else if(model.isMultiDay()) {
   var deadline=periods.generalDeadline(start.toLocalDate(),model.getMaxRentalDays(),calendar);
   candidates=start.toLocalDate().plusDays(1).datesUntil(deadline.toLocalDate().plusDays(1)).filter(d->!calendar.isClosed(d))
    .map(d->d.atTime(calendar.hours(d).close())).toList();
  } else candidates=new RentalSlotCalculator().slots(start.toLocalDate(),model.getIntervalMinutes(),calendar).stream().map(RentalSegment::end).toList();
  var existing=em.createQuery("select r from Rental r where r.modelId=:model and r.status in :statuses",Rental.class)
   .setParameter("model",model.getId()).setParameter("statuses",List.of(RentalStatus.RESERVED,RentalStatus.RENTING))
   .getResultList().stream().filter(r->!r.getId().equals(excludeId)).toList();
  var availableUnits=units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(model.getId());
  var calculator=new RentalCapacityCalculator(clock);
  return candidates.stream().filter(end->end.isAfter(after) && end.isAfter(start))
   .filter(end->model.getMaxRentalMinutes()==null || Duration.between(start,end).toMinutes()<=model.getMaxRentalMinutes())
   .filter(end->calculator.available(model.isCarryOut(),availableUnits,existing,new RentalSegment(excludeId==null?start:after,end))>0).toList();
 }
}
