package kr.ac.hansung.smartrent.domain.rental.service;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.*;
import kr.ac.hansung.smartrent.domain.rental.dto.*;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.domain.notification.entity.*;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.global.exception.*;
import kr.ac.hansung.smartrent.global.security.StudentAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class ReservationService {
 private final EntityManager em;
 private final RentalModelLockRepository locks;
 private final RentalCalendarReader calendars;
 private final RentalCapacityService capacity;
 private final RentalApplicationRepository applications;
 private final RentalRepository rentals;
 private final RentalReader reader;
 private final StudentAccessGuard guard;
 private final NotificationService notifications;
 private final Clock clock;
 public record Request(Long modelId,List<Segment> segments,LocalDate pickupDate) { }
 public record Segment(LocalDateTime start,LocalDateTime end) { }
 public record Result(Long applicationId,List<RentalResponse> rentals) { }
 public Result reserve(Long userId,Request request) {
  if(request.modelId()==null) fail(ErrorCode.VALIDATION_ERROR);
  var user=em.find(User.class,userId,LockModeType.PESSIMISTIC_WRITE);
  if(user==null) fail(ErrorCode.UNAUTHORIZED);
  guard.check(user);
  var model=locks.lockById(request.modelId()).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  if(model.isArchived()) fail(ErrorCode.MODEL_ARCHIVED);
  checkStudent(userId,model);
  var calendar=calendars.read(model);
  var now=LocalDateTime.now(clock);
  var periods=new RentalPeriodCalculator(clock);
  List<RentalSegment> segments;
  if(model.getRentalType()==RentalType.LONG_TERM) {
   if(request.pickupDate()==null || request.segments()!=null) fail(ErrorCode.VALIDATION_ERROR);
   var date=request.pickupDate();
   if(date.isBefore(now.toLocalDate()) || date.isAfter(now.toLocalDate().plusYears(1))) fail(ErrorCode.OUT_OF_BOOKING_WINDOW);
   if(calendar.isClosed(date)) fail(ErrorCode.CLOSED_DAY);
   if(!date.atTime(calendar.hours(date).close()).isAfter(now)) fail(ErrorCode.OUT_OF_BOOKING_WINDOW);
   segments=List.of(new RentalSegment(date.atTime(calendar.hours(date).open()),periods.longTermDeadline(date,calendar)));
  } else {
   if(request.pickupDate()!=null || request.segments()==null) fail(ErrorCode.VALIDATION_ERROR);
   if(request.segments().isEmpty()) fail(ErrorCode.SEGMENT_INVALID);
   segments=request.segments().stream().map(s->{
    if(s==null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    return new RentalSegment(s.start(),s.end());
   }).sorted(Comparator.comparing(RentalSegment::start)).toList();
   for(int i=0;i<segments.size();i++) {
    var s=segments.get(i);
    if(i>0 && s.start().isBefore(segments.get(i-1).end())) fail(ErrorCode.SEGMENTS_OVERLAP);
    if(!s.start().isAfter(now)) fail(ErrorCode.OUT_OF_BOOKING_WINDOW);
    if(calendar.isClosed(s.start().toLocalDate())) fail(ErrorCode.CLOSED_DAY);
    if(s.start().toLocalDate().isAfter(periods.nextFriday())) fail(ErrorCode.OUT_OF_BOOKING_WINDOW);
    if(!model.isMultiDay() && !s.start().toLocalDate().equals(s.end().toLocalDate())) fail(ErrorCode.SAME_DAY_ONLY);
    if(model.isMultiDay()) periods.checkReturnDate(s.end().toLocalDate(),calendar);
    if(!boundary(s.start(),model,calendar) || !boundary(s.end(),model,calendar)) fail(ErrorCode.SLOT_INVALID);
    if(model.isMultiDay() && s.end().toLocalDate().isAfter(s.start().toLocalDate())
     && !s.end().toLocalTime().equals(calendar.hours(s.end().toLocalDate()).close())) fail(ErrorCode.SLOT_INVALID);
    periods.checkDeadline(s.end(),periods.generalDeadline(s.start().toLocalDate(),model.getMaxRentalDays(),calendar));
   }
   periods.checkMinutes(segments,model.getMaxRentalMinutes());
  }
  for(var s:segments) if(capacity.available(model,s)<1) fail(ErrorCode.NO_CAPACITY);
  var app=applications.save(RentalApplication.builder().userId(userId).modelId(model.getId()).source(ApplicationSource.RESERVATION).build());
  var result=segments.stream().map(s->rentals.save(Rental.builder().applicationId(app.getId()).userId(userId).modelId(model.getId())
   .status(RentalStatus.RESERVED).startAt(s.start()).endAt(s.end()).build())).toList();
  notifications.notify(userId,NotificationKind.BOOKING,"예약 완료","예약이 완료되었습니다.",RelatedType.RENTAL,result.getFirst().getId());
  return new Result(app.getId(),result.stream().map(reader::response).toList());
 }
 public void checkStudent(Long userId,EquipmentModel model) {
  var now=LocalDateTime.now(clock);
  if(em.createQuery("select count(r) from Rental r where r.userId=:id and r.status=:status and r.endAt<:now",Long.class)
   .setParameter("id",userId).setParameter("status",RentalStatus.RENTING).setParameter("now",now).getSingleResult()>0)
   fail(ErrorCode.LATE_RENTAL_EXISTS);
  if(em.createQuery("select count(r) from Rental r join EquipmentModel m on m.id=r.modelId where r.userId=:id and r.status in :statuses and (m.id=:model or lower(m.name)=lower(:name))",Long.class)
   .setParameter("id",userId).setParameter("statuses",List.of(RentalStatus.RESERVED,RentalStatus.RENTING))
   .setParameter("model",model.getId()).setParameter("name",model.getName()).getSingleResult()>0) fail(ErrorCode.SAME_KIND_ACTIVE);
 }
 public static boolean boundary(LocalDateTime at,EquipmentModel model,RentalCalendar calendar) {
  var hours=calendar.hours(at.toLocalDate()); var open=at.toLocalDate().atTime(hours.open()); var close=at.toLocalDate().atTime(hours.close());
  return !at.isBefore(open) && !at.isAfter(close) && at.getSecond()==0 && at.getNano()==0
   && Duration.between(open,at).toMinutes()%model.getIntervalMinutes()==0;
 }
 private static void fail(ErrorCode code) { throw new BusinessException(code); }
}
