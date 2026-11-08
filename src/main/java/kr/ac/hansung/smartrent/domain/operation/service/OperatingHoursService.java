package kr.ac.hansung.smartrent.domain.operation.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.operation.dto.HoursItemResponse;
import kr.ac.hansung.smartrent.domain.operation.dto.HoursWriteRequest;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.domain.operation.entity.OperatingHours;
import kr.ac.hansung.smartrent.domain.operation.repository.OperatingHoursRepository;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모델별 운영 시간 변경(B1-20, 기획안 8장, DB 설계 3·4절 operating_hours).
 * 그날 운영 시간은 날짜 변경 → 기본 변경 → 모델의 open_time/close_time 순서이고, 대여 쪽 RentalCalendarReader가 이 표를 읽습니다.
 * 바꾸거나 되돌린 뒤 같은 트랜잭션에서 B2-16 OperationChangeService.hoursChanged로 벗어나는 예약을 신청 순서와 관계없이 모두 취소합니다.
 */
@Service
@RequiredArgsConstructor
public class OperatingHoursService {

	private final OperatingHoursRepository hoursRepository;
	private final EquipmentModelRepository modelRepository;
	private final RentalModelLockRepository modelLocks;
	private final OperationChangeService operationChangeService;
	private final AuditLogService auditLogService;
	private final EntityManager em;

	/** 기본 변경이 먼저, 그다음 날짜 순 */
	@Transactional(readOnly = true)
	public List<HoursItemResponse> list(Long modelId) {
		modelRepository.findById(modelId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		return hoursRepository.findAllByModelId(modelId).stream()
			.sorted(Comparator.comparing(OperatingHours::getDate, Comparator.nullsFirst(Comparator.naturalOrder())))
			.map(HoursItemResponse::of)
			.toList();
	}

	@Transactional
	public OperationChangeResponse change(Long modelId, HoursWriteRequest request, Long adminId) {
		LocalTime open = LocalTime.parse(request.openTime());
		LocalTime close = LocalTime.parse(request.closeTime());
		if (!open.isBefore(close)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "운영 시작 시각은 끝 시각보다 앞이어야 합니다.");
		}
		EquipmentModel model = modelLocks.lockById(modelId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		LocalDate date = request.date();
		(date == null ? hoursRepository.findByModelIdAndDateIsNull(modelId) : hoursRepository.findByModelIdAndDate(modelId, date))
			.ifPresentOrElse(h -> h.change(open, close), () -> hoursRepository.save(
				OperatingHours.builder().modelId(modelId).date(date).openTime(open).closeTime(close).build()));
		em.flush();
		OperationChangeService.Result result = operationChangeService.hoursChanged(modelId, date);
		auditLogService.record("OPERATIONS", "모델 " + model.getId() + " " + model.getName() + " 운영 시간 "
			+ (date == null ? "기본" : date.toString()) + " " + request.openTime() + "~" + request.closeTime()
			+ ", 취소 예약 " + result.cancelled().size() + "건", adminId);
		return OperationChangeResponse.of(result, em);
	}

	/** 날짜별 변경을 지워 기본 시간으로. 그 날짜 변경이 없으면 404 */
	@Transactional
	public OperationChangeResponse revert(Long modelId, LocalDate date, Long adminId) {
		EquipmentModel model = modelLocks.lockById(modelId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		OperatingHours hours = hoursRepository.findByModelIdAndDate(modelId, date)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		hoursRepository.delete(hours);
		em.flush();
		OperationChangeService.Result result = operationChangeService.hoursChanged(modelId, date);
		auditLogService.record("OPERATIONS", "모델 " + model.getId() + " " + model.getName() + " 운영 시간 " + date
			+ " 되돌림, 취소 예약 " + result.cancelled().size() + "건", adminId);
		return OperationChangeResponse.of(result, em);
	}
}
