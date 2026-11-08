package kr.ac.hansung.smartrent.domain.equipment.dto;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalCalendar;

/** openapi.yaml AdminModelDetail: ModelDetail + 개별 기기(지금 대여 중인지 포함)·운영 종료 여부·기본 운영 시간 */
public record AdminModelDetailResponse(Long id, String category, String name, String manufacturer, String purchaseYear,
	String purpose, RentalType rentalType, boolean multiDay, Integer maxRentalDays, Integer maxRentalMinutes,
	boolean carryOut, String location, String contact, ModelDetailResponse.Hours todayHours, int intervalMinutes,
	String guide, List<String> imageUrls, long normalUnits, long availableNow, ModelSource source, List<Unit> units,
	boolean archived, String openTime, String closeTime) {

	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

	public record Unit(Long id, String code, String name, UnitCondition condition, boolean inUse) {
	}

	/**
	 * @param today       오늘 운영 시간. 쉬는 날이면 null
	 * @param inUseUnits  지금 RENTING 대여에 묶인 기기 id
	 */
	public static AdminModelDetailResponse of(EquipmentModel m, RentalCalendar.Hours today, List<String> imageUrls,
		List<EquipmentUnit> units, Set<Long> inUseUnits, long availableNow) {
		return new AdminModelDetailResponse(m.getId(), m.getCategory(), m.getName(), m.getManufacturer(),
			m.getPurchaseYear(), m.getPurpose(), m.getRentalType(), m.isMultiDay(), m.getMaxRentalDays(),
			m.getMaxRentalMinutes(), m.isCarryOut(), m.getLocation(), m.getContact(),
			today == null ? null : new ModelDetailResponse.Hours(today.open().format(HM), today.close().format(HM)),
			m.getIntervalMinutes(), m.getGuide(), imageUrls,
			units.stream().filter(u -> u.getCondition() == UnitCondition.NORMAL).count(), availableNow, m.getSource(),
			units.stream().map(u -> new Unit(u.getId(), u.getCode(), u.getName(), u.getCondition(),
				inUseUnits.contains(u.getId()))).toList(),
			m.isArchived(), m.getOpenTime().format(HM), m.getCloseTime().format(HM));
	}
}
