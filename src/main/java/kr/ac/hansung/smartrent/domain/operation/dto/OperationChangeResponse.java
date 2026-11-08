package kr.ac.hansung.smartrent.domain.operation.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.domain.user.entity.User;

/**
 * openapi.yaml OperationChangeResult. 운영 변경(모델 운영 종료·운영 시간·휴무일·기기 수량)으로 취소되거나 기한이 늘어난 대여.
 * 관리자가 학생에게 직접 연락할 수 있게 돌려줍니다. 예약 정리는 B2-16 OperationChangeService가 하고 여기서는 모양만 바꿉니다.
 */
public record OperationChangeResponse(List<Cancelled> cancelledRentals, List<Extended> extendedRentals) {

	public record Cancelled(Long rentalId, String studentName, String modelName, LocalDateTime startAt) {
	}

	public record Extended(Long rentalId, LocalDateTime newEndAt) {
	}

	public static OperationChangeResponse empty() {
		return new OperationChangeResponse(List.of(), List.of());
	}

	public static OperationChangeResponse of(OperationChangeService.Result result, EntityManager em) {
		return new OperationChangeResponse(
			result.cancelled().stream().map(r -> new Cancelled(r.id(), studentName(r, em), r.modelName(), r.startAt())).toList(),
			result.extended().stream().map(r -> new Extended(r.id(), r.endAt())).toList());
	}

	private static String studentName(RentalResponse response, EntityManager em) {
		Rental rental = em.find(Rental.class, response.id());
		User user = rental == null ? null : em.find(User.class, rental.getUserId());
		return user == null ? null : user.getName();
	}
}
