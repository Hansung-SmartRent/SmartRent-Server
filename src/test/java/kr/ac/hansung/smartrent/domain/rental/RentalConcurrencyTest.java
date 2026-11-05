package kr.ac.hansung.smartrent.domain.rental;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import jakarta.persistence.LockModeType;
import kr.ac.hansung.smartrent.support.Backend2Support;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
class RentalConcurrencyTest extends Backend2Support {
 @Autowired ReservationService reservations;
 @Autowired OperationChangeService changes;
 @Autowired RentalModelLockRepository locks;
 @Autowired PlatformTransactionManager manager;
 Long modelId,unitId;
 @BeforeEach void createModel() {
  new TransactionTemplate(manager).executeWithoutResult(s->{
   var model=EquipmentModel.builder().source(ModelSource.EXAMPLE).modelKey("b2-concurrency")
    .name("Creality k1 Max").category("3D 프린터").location("").rentalType(RentalType.GENERAL)
    .openTime(LocalTime.of(9,0)).closeTime(LocalTime.of(20,0)).intervalMinutes(60).build();
   em.persist(model); modelId=model.getId();
   var unit=EquipmentUnit.builder().modelId(modelId).code("b2-concurrency-unit").name("Creality k1 Max 1번").condition(UnitCondition.NORMAL).build();
   em.persist(unit); unitId=unit.getId();
  });
 }
 @AfterEach void cleanup() {
  new TransactionTemplate(manager).executeWithoutResult(s->{
   em.createQuery("delete from Rental where modelId=:id").setParameter("id",modelId).executeUpdate();
   em.createQuery("delete from RentalApplication where modelId=:id").setParameter("id",modelId).executeUpdate();
   em.createQuery("delete from EquipmentUnit where modelId=:id").setParameter("id",modelId).executeUpdate();
   em.createQuery("delete from EquipmentModel where id=:id").setParameter("id",modelId).executeUpdate();
  });
 }
 String reserve(Long user) {
  try {
   reservations.reserve(user,new ReservationService.Request(modelId,List.of(new ReservationService.Segment(
    LocalDateTime.of(2026,10,7,10,0),LocalDateTime.of(2026,10,7,11,0))),null));
   return "SUCCESS";
  } catch(BusinessException ex) { return ex.getErrorCode().name(); }
 }
 @Test void B2_03_last_unit_two_real_transactions() throws Exception {
  var gate=new CountDownLatch(1); var a=student(); var b=other();
  try(var pool=Executors.newFixedThreadPool(2)) {
   var first=pool.submit(()->{gate.await();return reserve(a);});
   var second=pool.submit(()->{gate.await();return reserve(b);}); gate.countDown();
   assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder("SUCCESS","NO_CAPACITY");
  }
 }
 @Test void B2_16_capacity_change_races_with_reservation() throws Exception {
  var gate=new CountDownLatch(1); var user=student();
  try(var pool=Executors.newFixedThreadPool(2)) {
   var reservation=pool.submit(()->{gate.await();return reserve(user);});
   var damage=pool.submit(()->{
    gate.await(); new TransactionTemplate(manager).executeWithoutResult(s->{
     locks.lockById(modelId).orElseThrow();
     em.find(EquipmentUnit.class,unitId).changeCondition(UnitCondition.DAMAGED);
     changes.capacityChanged(modelId);
    }); return "DONE";
   }); gate.countDown(); reservation.get(20,TimeUnit.SECONDS); damage.get(20,TimeUnit.SECONDS);
  }
  new TransactionTemplate(manager).executeWithoutResult(s->
   assertThat(em.createQuery("select count(r) from Rental r where r.modelId=:id and r.status=:status",Long.class)
    .setParameter("id",modelId).setParameter("status",RentalStatus.RESERVED).getSingleResult()).isZero());
 }
}
