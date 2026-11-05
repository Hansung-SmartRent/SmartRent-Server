package kr.ac.hansung.smartrent.domain.rental.service;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.warning.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Slf4j @Component @RequiredArgsConstructor
@ConditionalOnProperty(name="smartrent.rental.scheduler-enabled",havingValue="true",matchIfMissing=true)
public class RentalJudgmentScheduler {
 private final EntityManager em;
 private final RentalJudgmentService judgments;
 private final WarningService warnings;
 private final RetainedWarningService retained;
 @Scheduled(fixedDelay=60000,initialDelay=60000)
 public void run() {
  for(var id:em.createQuery("select distinct r.modelId from Rental r where r.status in (kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus.RESERVED,kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus.RENTING) order by r.modelId",Long.class).getResultList()) {
   try { judgments.processModel(id); } catch(RuntimeException ex) { log.error("대여 판정 실패: 모델 id {}",id,ex); }
  }
  warnings.expireSuspensions();
 }
 @Scheduled(cron="0 0 3 * * *",zone="Asia/Seoul")
 public void purge() { retained.purgeExpired(); }
}
