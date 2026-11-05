package kr.ac.hansung.smartrent.support;
import java.time.*;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import kr.ac.hansung.smartrent.domain.rental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
@Transactional
public abstract class RentalSupport extends Backend2Support {
 @Autowired protected ReservationService reservations;
 @Autowired protected RentalReader reader;
 @Autowired protected RentalRepository rentals;
 protected EquipmentModel model(String key) {
  return em.createQuery("select m from EquipmentModel m where m.modelKey=:key",EquipmentModel.class)
   .setParameter("key",key).getSingleResult();
 }
 protected EquipmentUnit unit(EquipmentModel m) {
  return em.createQuery("select u from EquipmentUnit u where u.modelId=:id and u.condition=:normal and u.deletedAt is null order by u.id",EquipmentUnit.class)
   .setParameter("id",m.getId()).setParameter("normal",UnitCondition.NORMAL).setMaxResults(1).getSingleResult();
 }
 protected LocalDateTime time(String at) { return LocalDateTime.parse("2026-10-07T"+at+":00"); }
 protected Rental record(EquipmentModel m,Long user,RentalStatus status,String start,String end) {
  var app=RentalApplication.builder().userId(user).modelId(m.getId()).source(ApplicationSource.RESERVATION).build();
  em.persist(app);
  var r=Rental.builder().applicationId(app.getId()).userId(user).modelId(m.getId()).status(status)
   .startAt(time(start)).endAt(time(end)).pickedAt(status==RentalStatus.RENTING?time(start):null)
   .unitId(status==RentalStatus.RENTING?unit(m).getId():null).build();
  em.persist(r); return r;
 }
 protected void clearRentals() {
  em.createQuery("delete from Warning where rentalId is not null").executeUpdate();
  em.createQuery("delete from Rental").executeUpdate();
  em.createQuery("delete from RentalApplication").executeUpdate(); em.clear();
 }
}
