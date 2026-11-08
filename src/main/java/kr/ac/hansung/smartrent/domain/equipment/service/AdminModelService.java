package kr.ac.hansung.smartrent.domain.equipment.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.dto.ModelWriteRequest;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 모델 추가·수정·운영 종료(B1-17, 기획안 2·4·11장, DB 설계 3·4절).
 * 운영 중인 모델끼리 name_key(소문자, 공백 제거)가 같으면 MODEL_NAME_DUPLICATE.
 * 수정·운영 종료는 모델 잠금을 잡고 저장한 뒤 같은 트랜잭션에서 B2-16 OperationChangeService를 부릅니다.
 */
@Service
@RequiredArgsConstructor
public class AdminModelService {

	private static final int DETAIL_MAX = 500;

	private final EquipmentModelRepository modelRepository;
	private final RentalModelLockRepository modelLocks;
	private final OperationChangeService operationChangeService;
	private final ModelQueryService modelQueryService;
	private final AuditLogService auditLogService;
	private final EntityManager em;

	@Transactional(readOnly = true)
	public AdminModelDetailResponse detail(Long id) {
		return modelQueryService.adminDetail(id);
	}

	/** source=ADMIN, model_key는 서버가 만듦(DB 설계 3절) */
	@Transactional
	public AdminModelDetailResponse create(ModelWriteRequest request, Long adminId) {
		Times times = times(request);
		String name = request.name().trim();
		rejectDuplicateName(name, null);
		EquipmentModel model = modelRepository.save(EquipmentModel.builder()
			.source(ModelSource.ADMIN)
			.modelKey("admin-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
			.category(request.category().trim())
			.name(name)
			.nameKey(EquipmentModel.nameKeyOf(name))
			.manufacturer(blankToNull(request.manufacturer()))
			.purchaseYear(blankToNull(request.purchaseYear()))
			.purpose(blankToNull(request.purpose()))
			.rentalType(request.rentalType())
			.multiDay(request.multiDay())
			.maxRentalDays(request.maxRentalDays())
			.maxRentalMinutes(request.maxRentalMinutes())
			.carryOut(request.carryOut())
			.location(request.location().trim())
			.contact(blankToNull(request.contact()))
			.openTime(times.open())
			.closeTime(times.close())
			.intervalMinutes(request.intervalMinutes())
			.guide(blankToNull(request.guide()))
			.build());
		auditLogService.record("MODEL_ADD", "모델 " + model.getId() + " " + model.getName() + " 추가", adminId);
		return modelQueryService.adminDetail(model);
	}

	/** 운영 시간 기본값이 바뀌면 그 밖으로 나가는 예약을 경고 없이 취소하고 목록을 돌려줌 */
	@Transactional
	public OperationChangeResponse update(Long id, ModelWriteRequest request, Long adminId) {
		Times times = times(request);
		EquipmentModel model = modelLocks.lockById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		String name = request.name().trim();
		if (!model.isArchived()) {
			rejectDuplicateName(name, model.getId());
		}
		List<String> changes = new ArrayList<>();
		diff(changes, "category", model.getCategory(), request.category().trim());
		diff(changes, "name", model.getName(), name);
		diff(changes, "manufacturer", model.getManufacturer(), blankToNull(request.manufacturer()));
		diff(changes, "purchaseYear", model.getPurchaseYear(), blankToNull(request.purchaseYear()));
		diff(changes, "purpose", model.getPurpose(), blankToNull(request.purpose()));
		diff(changes, "rentalType", model.getRentalType(), request.rentalType());
		diff(changes, "multiDay", model.isMultiDay(), request.multiDay());
		diff(changes, "maxRentalDays", model.getMaxRentalDays(), request.maxRentalDays());
		diff(changes, "maxRentalMinutes", model.getMaxRentalMinutes(), request.maxRentalMinutes());
		diff(changes, "carryOut", model.isCarryOut(), request.carryOut());
		diff(changes, "location", model.getLocation(), request.location().trim());
		diff(changes, "contact", model.getContact(), blankToNull(request.contact()));
		diff(changes, "openTime", model.getOpenTime(), times.open());
		diff(changes, "closeTime", model.getCloseTime(), times.close());
		diff(changes, "intervalMinutes", model.getIntervalMinutes(), request.intervalMinutes());
		boolean hoursChanged = !model.getOpenTime().equals(times.open()) || !model.getCloseTime().equals(times.close());

		model.update(request.category().trim(), name, blankToNull(request.manufacturer()),
			blankToNull(request.purchaseYear()), blankToNull(request.purpose()), request.rentalType(), request.multiDay(),
			request.maxRentalDays(), request.maxRentalMinutes(), request.carryOut(), request.location().trim(),
			blankToNull(request.contact()), times.open(), times.close(), request.intervalMinutes(),
			blankToNull(request.guide()));
		if (!changes.isEmpty()) {
			auditLogService.record("MODEL_EDIT", limit("모델 " + model.getId() + ": " + String.join(", ", changes)), adminId);
		}
		if (!hoursChanged || model.isArchived()) {
			return OperationChangeResponse.empty();
		}
		em.flush();
		return OperationChangeResponse.of(operationChangeService.hoursChanged(model.getId(), null), em);
	}

	/** 대여 중이면 MODEL_ARCHIVE_BLOCKED(B2-16 서비스가 검사). 앞으로의 예약은 경고 없이 취소하고 알림 */
	@Transactional
	public OperationChangeResponse archive(Long id, Long adminId) {
		EquipmentModel model = modelLocks.lockById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		if (model.isArchived()) {
			return OperationChangeResponse.empty();
		}
		OperationChangeService.Result result = operationChangeService.modelArchived(model.getId());
		model.archive();
		auditLogService.record("MODEL_ARCHIVE",
			"모델 " + model.getId() + " " + model.getName() + " 운영 종료, 취소 예약 " + result.cancelled().size() + "건", adminId);
		return OperationChangeResponse.of(result, em);
	}

	private void rejectDuplicateName(String name, Long selfId) {
		modelRepository.findByNameKeyAndArchivedFalse(EquipmentModel.nameKeyOf(name))
			.filter(other -> !other.getId().equals(selfId))
			.ifPresent(other -> {
				throw new BusinessException(ErrorCode.MODEL_NAME_DUPLICATE);
			});
	}

	private record Times(LocalTime open, LocalTime close) {
	}

	/** 형식은 요청 검사에서 봤고, 여기서는 시작이 끝보다 앞인지와 간격(30·60분)을 봄 */
	private static Times times(ModelWriteRequest request) {
		LocalTime open = LocalTime.parse(request.openTime());
		LocalTime close = LocalTime.parse(request.closeTime());
		if (!open.isBefore(close)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "운영 시작 시각은 끝 시각보다 앞이어야 합니다.");
		}
		if (request.intervalMinutes() != 30 && request.intervalMinutes() != 60) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "예약 간격은 30분 또는 60분입니다.");
		}
		return new Times(open, close);
	}

	private static void diff(List<String> changes, String field, Object before, Object after) {
		if (!Objects.equals(before, after)) {
			changes.add(field + " " + before + " -> " + after);
		}
	}

	private static String limit(String detail) {
		return detail.length() <= DETAIL_MAX ? detail : detail.substring(0, DETAIL_MAX);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
