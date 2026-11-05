package kr.ac.hansung.smartrent.domain.rental.dto;
import java.time.LocalDateTime;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
public record RentalResponse(Long id,Long applicationId,ApplicationSource source,Long modelId,String modelName,
 RentalType rentalType,UnitBrief unit,RentalStatus status,LocalDateTime startAt,LocalDateTime endAt,
 LocalDateTime pickedAt,LocalDateTime returnedAt,boolean extended,boolean brokenReported,boolean returnRequired,
 String cancelReason,boolean canCancel,boolean canExtend,String brokenNote,ReturnCondition returnCondition,
 LocalDateTime damageDiscoveredAt) {
 public record UnitBrief(Long id,String code,String name) { }
}
