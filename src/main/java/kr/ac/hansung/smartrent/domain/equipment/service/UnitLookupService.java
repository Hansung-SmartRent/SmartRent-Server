package kr.ac.hansung.smartrent.domain.equipment.service;

import kr.ac.hansung.smartrent.domain.equipment.dto.ScanResultResponse;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기기 코드로 찾기(B1-11). 학생 스캔과 관리자의 수령·반납 스캔이 함께 씁니다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitLookupService {

	private final EquipmentUnitRepository unitRepository;
	private final EquipmentModelRepository modelRepository;
	private final UserRepository userRepository;
	private final ModelQueryService modelQueryService;

	/** 지운 기기는 없는 것과 같이 404 */
	public ScanResultResponse byCode(String code, Long userId) {
		EquipmentUnit unit = unitRepository.findByCodeAndDeletedAtIsNull(code.trim())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		EquipmentModel model = modelRepository.findById(unit.getModelId())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		boolean admin = userRepository.findById(userId).map(u -> u.getRole() == Role.ADMIN).orElse(false);
		return new ScanResultResponse(modelQueryService.summary(model), admin ? ScanResultResponse.Unit.of(unit) : null);
	}
}
