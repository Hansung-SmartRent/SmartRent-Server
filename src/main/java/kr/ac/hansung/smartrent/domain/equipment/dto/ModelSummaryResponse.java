package kr.ac.hansung.smartrent.domain.equipment.dto;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;

/** openapi.yaml ModelSummary. 학생·관리자 공용 목록 한 줄 */
public record ModelSummaryResponse(Long id, String category, String name, RentalType rentalType, boolean multiDay,
	Integer maxRentalDays, Integer maxRentalMinutes, boolean carryOut, String location, String thumbnailUrl,
	long normalUnits, long availableNow, ModelSource source) {

	public static ModelSummaryResponse of(EquipmentModel m, String thumbnailUrl, long normalUnits, long availableNow) {
		return new ModelSummaryResponse(m.getId(), m.getCategory(), m.getName(), m.getRentalType(), m.isMultiDay(),
			m.getMaxRentalDays(), m.getMaxRentalMinutes(), m.isCarryOut(), m.getLocation(), thumbnailUrl, normalUnits,
			availableNow, m.getSource());
	}
}
