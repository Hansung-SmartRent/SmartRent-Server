package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import kr.ac.hansung.smartrent.domain.rental.dto.AdminRentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.user.entity.*;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminRentalService {
 private final EntityManager em;
 private final RentalLookupService lookup;
 private final Clock clock;
 private static final String FROM=" from Rental r join User u on u.id=r.userId join EquipmentModel m on m.id=r.modelId left join EquipmentUnit t on t.id=r.unitId";
 private static final String FILTER=" where (:status is null or r.status=:status) and (:q is null or lower(u.name) like :q or u.studentNumber like :q or lower(m.name) like :q or lower(t.code) like :q)";
 public PageResponse<AdminRentalResponse> list(RentalStatus status,String q,int page,int size) {
  String needle=q==null || q.isBlank()?null:"%"+q.trim().toLowerCase(Locale.ROOT)+"%";
  long count=em.createQuery("select count(r)"+FROM+FILTER,Long.class).setParameter("status",status).setParameter("q",needle).getSingleResult();
  var result=em.createQuery("select r"+FROM+FILTER+" order by r.createdAt desc,r.id desc",Rental.class)
   .setParameter("status",status).setParameter("q",needle).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList();
  return new PageResponse<>(result.stream().map(lookup::response).toList(),new PageResponse.Meta(page,size,count,(count+size-1)/size));
 }
 public record Priority(String type,String title,String subtitle,Long targetId) { }
 public record Summary(long pendingStudents,long pickupWaiting,long renting,List<Priority> priorities) { }
 public Summary summary() {
  var now=LocalDateTime.now(clock); var today=now.toLocalDate();
  long pending=em.createQuery("select count(u) from User u where u.role=:role and u.approval=:approval",Long.class)
   .setParameter("role",Role.STUDENT).setParameter("approval",Approval.PENDING).getSingleResult();
  long pickups=em.createQuery("select count(r) from Rental r where r.status=:status and r.startAt>=:start and r.startAt<:end",Long.class)
   .setParameter("status",RentalStatus.RESERVED).setParameter("start",today.atStartOfDay()).setParameter("end",today.plusDays(1).atStartOfDay()).getSingleResult();
  long renting=em.createQuery("select count(r) from Rental r where r.status=:status",Long.class).setParameter("status",RentalStatus.RENTING).getSingleResult();
  var priorities=new ArrayList<Priority>();
  for(var row:em.createQuery("select r,u.name,m.name from Rental r join User u on u.id=r.userId join EquipmentModel m on m.id=r.modelId where r.status=:renting and r.endAt<:now order by r.endAt,r.id",Object[].class)
   .setParameter("renting",RentalStatus.RENTING).setParameter("now",now).setMaxResults(20).getResultList()) {
   var r=(Rental)row[0]; priorities.add(new Priority("LATE","연체: "+row[1]+" · "+row[2],"반납 기한 "+r.getEndAt(),r.getId()));
  }
  for(var r:em.createQuery("select r from Rental r where r.status=:status and r.brokenReported=true order by r.id",Rental.class)
   .setParameter("status",RentalStatus.RENTING).setMaxResults(20).getResultList())
   priorities.add(new Priority("DAMAGE_REPORT","파손 신고",r.getBrokenNote(),r.getId()));
  for(var r:em.createQuery("select r from Rental r where r.status=:status and r.startAt>=:start and r.startAt<:end order by r.startAt,r.id",Rental.class)
   .setParameter("status",RentalStatus.RESERVED).setParameter("start",today.atStartOfDay()).setParameter("end",today.plusDays(1).atStartOfDay()).setMaxResults(20).getResultList())
   priorities.add(new Priority("PICKUP_SOON","수령 예정","수령 시각 "+r.getStartAt(),r.getId()));
  for(var u:em.createQuery("select u from User u where u.role=:role and u.approval=:approval order by u.createdAt,u.id",User.class)
   .setParameter("role",Role.STUDENT).setParameter("approval",Approval.PENDING).setMaxResults(20).getResultList())
   priorities.add(new Priority("STUDENT_PENDING","가입 승인 대기",u.getName(),u.getId()));
  for(var row:em.createQuery("select i.id,i.title from Inquiry i where i.response is null order by i.createdAt,i.id",Object[].class).setMaxResults(20).getResultList())
   priorities.add(new Priority("INQUIRY","문의 답변 대기",(String)row[1],(Long)row[0]));
  return new Summary(pending,pickups,renting,List.copyOf(priorities));
 }
}
