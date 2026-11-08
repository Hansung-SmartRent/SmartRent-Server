package kr.ac.hansung.smartrent.domain.equipment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.dto.UnitPatchRequest;
import kr.ac.hansung.smartrent.domain.equipment.dto.UnitWriteRequest;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalModelLockRepository;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 기기 추가·이름·상태 변경·삭제(B1-18, 기획안 2장, DB 설계 3·4절).
 * 기기 코드는 지운 기기까지 포함해 유일(DB uk_equipment_units_code). 상태를 점검 중·파손으로 바꾸거나 삭제하면
 * 모델 잠금을 잡고 저장한 뒤 같은 트랜잭션에서 B2-16 OperationChangeService.capacityChanged로 대수를 다시 셉니다.
 */
@Service
@RequiredArgsConstructor
public class AdminUnitService {

	private final EquipmentUnitRepository unitRepository;
	private final RentalModelLockRepository modelLocks;
	private final OperationChangeService operationChangeService;
	private final AuditLogService auditLogService;
	private final EntityManager em;
	private final Clock clock;

	@Transactional
	public AdminModelDetailResponse.Unit add(Long modelId, UnitWriteRequest request, Long adminId) {
		EquipmentModel model = modelLocks.lockById(modelId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		String code = request.code().trim();
		if (em.createQuery("select count(u) from EquipmentUnit u where u.code = :code", Long.class)
			.setParameter("code", code).getSingleResult() > 0) {
			throw new BusinessException(ErrorCode.UNIT_CODE_DUPLICATE);
		}
		EquipmentUnit unit = unitRepository.save(EquipmentUnit.builder().modelId(model.getId()).code(code)
			.name(request.name().trim()).condition(UnitCondition.NORMAL).build());
		auditLogService.record("CAPACITY", "모델 " + model.getId() + " 기기 " + code + " 추가", adminId);
		return new AdminModelDetailResponse.Unit(unit.getId(), unit.getCode(), unit.getName(), unit.getCondition(), false);
	}

	/** 수리가 끝나 NORMAL로 되돌리면 예약 정리 없음. INSPECTION·DAMAGED가 되면 줄 수 없는 예약만 늦게 신청한 순서로 취소 */
	@Transactional
	public OperationChangeResponse patch(Long unitId, UnitPatchRequest request, Long adminId) {
		EquipmentUnit unit = lockedUnit(unitId);
		if (request.name() != null && !request.name().trim().equals(unit.getName())) {
			auditLogService.record("UNIT_EDIT", "기기 " + unit.getCode() + " 이름: " + unit.getName() + " -> " + request.name().trim(), adminId);
			unit.rename(request.name().trim());
		}
		if (request.condition() == null || request.condition() == unit.getCondition()) {
			return OperationChangeResponse.empty();
		}
		auditLogService.record("UNIT_CONDITION",
			"기기 " + unit.getCode() + ": " + unit.getCondition() + " -> " + request.condition(), adminId);
		unit.changeCondition(request.condition());
		if (request.condition() == UnitCondition.NORMAL) {
			return OperationChangeResponse.empty();
		}
		return OperationChangeResponse.of(operationChangeService.capacityChanged(unit.getModelId()), em);
	}

	/** 대여 중이면 UNIT_DELETE_BLOCKED. deleted_at 표시 후 대수가 모자라게 된 예약은 경고 없이 취소 */
	@Transactional
	public OperationChangeResponse delete(Long unitId, Long adminId) {
		EquipmentUnit unit = lockedUnit(unitId);
		if (em.createQuery("select count(r) from Rental r where r.unitId = :id and r.status = :status", Long.class)
			.setParameter("id", unit.getId()).setParameter("status", RentalStatus.RENTING).getSingleResult() > 0) {
			throw new BusinessException(ErrorCode.UNIT_DELETE_BLOCKED);
		}
		unit.delete(LocalDateTime.now(clock));
		OperationChangeService.Result result = operationChangeService.capacityChanged(unit.getModelId());
		auditLogService.record("CAPACITY", "모델 " + unit.getModelId() + " 기기 " + unit.getCode() + " 삭제, 취소 예약 "
			+ result.cancelled().size() + "건", adminId);
		return OperationChangeResponse.of(result, em);
	}

	/** 지운 기기는 없는 것과 같이 404. 모델 잠금을 먼저 잡고 기기를 다시 읽음(DB 6절) */
	private EquipmentUnit lockedUnit(Long unitId) {
		EquipmentUnit found = unitRepository.findById(unitId).filter(u -> u.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		modelLocks.lockById(found.getModelId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		em.refresh(found);
		if (found.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.NOT_FOUND);
		}
		return found;
	}
}
