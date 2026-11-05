package kr.ac.hansung.smartrent.domain.equipment.dto;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;

/** openapi.yaml ScanResult. 학생에게는 모델만, 관리자에게는 기기 상태까지(unit) 줍니다 */
public record ScanResultResponse(ModelSummaryResponse model, Unit unit) {

	public record Unit(Long id, String code, String name, UnitCondition condition) {
		public static Unit of(EquipmentUnit u) {
			return new Unit(u.getId(), u.getCode(), u.getName(), u.getCondition());
		}
	}
}
