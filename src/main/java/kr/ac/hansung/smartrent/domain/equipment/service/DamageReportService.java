package kr.ac.hansung.smartrent.domain.equipment.service;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationKind;
import kr.ac.hansung.smartrent.domain.notification.entity.RelatedType;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.domain.rental.service.RentalReader;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기기 이상 신고(B1-13, 기획안 2·7장, DB 설계 4절).
 * 본인의 RENTING 대여만. 모델 잠금을 잡고 대여에 신고 표시, 기기는 INSPECTION(대여는 RENTING·반납 기한 그대로).
 * 같은 트랜잭션에서 B2-16 OperationChangeService.capacityChanged로 대수를 다시 세어 줄 수 없는 예약만 늦게 신청한 순서로 취소합니다.
 * 취소 결과는 그 서비스가 예약자에게 CANCEL 알림을 보내는 데만 쓰고, 신고자 응답에는 본인 대여 하나만 넣습니다.
 */
@Service
@RequiredArgsConstructor
public class DamageReportService {

	private final RentalReader rentalReader;
	private final RentalModelLockRepository modelLocks;
	private final OperationChangeService operationChangeService;
	private final NotificationService notificationService;
	private final EntityManager em;

	@Transactional
	public RentalResponse report(Long rentalId, Long userId, String note) {
		Rental rental = rentalReader.owned(rentalId, userId, false);
		EquipmentModel model = modelLocks.lockById(rental.getModelId())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		em.refresh(rental);
		if (rental.getStatus() != RentalStatus.RENTING || rental.getUnitId() == null) {
			throw new BusinessException(ErrorCode.INVALID_STATUS);
		}
		String trimmed = note.trim();
		rental.reportBroken(trimmed);
		em.find(EquipmentUnit.class, rental.getUnitId()).changeCondition(UnitCondition.INSPECTION);
		operationChangeService.capacityChanged(model.getId());

		User student = em.find(User.class, userId);
		notificationService.toAdmins(NotificationKind.DAMAGE, "파손 신고",
			student.getName() + " · " + model.getName() + ": " + trimmed, RelatedType.RENTAL, rental.getId());
		return rentalReader.response(rental);
	}
}
