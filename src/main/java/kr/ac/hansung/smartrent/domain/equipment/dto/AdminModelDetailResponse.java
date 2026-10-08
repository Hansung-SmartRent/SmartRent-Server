package kr.ac.hansung.smartrent.domain.equipment.dto;

import java.time.format.DateTimeFormatter;
import java.util.List;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;

/**
 * openapi.yaml AdminModelDetail 중 지금 채울 수 있는 칸. 모델 사진 올리기(B1-08) 응답.
 * todayHours·availableNow·units[].inUse는 운영 시간·대여 수 계산이 필요해 관리자 모델 상세(B1-17)에서 채웁니다.
 */
public record AdminModelDetailResponse(Long id, String category, String name, String manufacturer, String purchaseYear,
	String purpose, RentalType rentalType, boolean multiDay, Integer maxRentalDays, Integer maxRentalMinutes,
	boolean carryOut, String location, String contact, int intervalMinutes, String guide, List<String> imageUrls,
	long normalUnits, ModelSource source, List<Unit> units, boolean archived, String openTime, String closeTime) {

	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

	public record Unit(Long id, String code, String name, UnitCondition condition) {
	}

	public static AdminModelDetailResponse of(EquipmentModel m, List<String> imageUrls, List<EquipmentUnit> units) {
		return new AdminModelDetailResponse(m.getId(), m.getCategory(), m.getName(), m.getManufacturer(),
			m.getPurchaseYear(), m.getPurpose(), m.getRentalType(), m.isMultiDay(), m.getMaxRentalDays(),
			m.getMaxRentalMinutes(), m.isCarryOut(), m.getLocation(), m.getContact(), m.getIntervalMinutes(),
			m.getGuide(), imageUrls, units.stream().filter(u -> u.getCondition() == UnitCondition.NORMAL).count(),
			m.getSource(), units.stream().map(u -> new Unit(u.getId(), u.getCode(), u.getName(), u.getCondition())).toList(),
			m.isArchived(), m.getOpenTime().format(HM), m.getCloseTime().format(HM));
	}
}
