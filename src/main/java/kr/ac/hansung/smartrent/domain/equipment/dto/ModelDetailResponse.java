package kr.ac.hansung.smartrent.domain.equipment.dto;

import java.time.format.DateTimeFormatter;
import java.util.List;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalCalendar;

/** openapi.yaml ModelDetail. 학생용 상세라 개별 기기 목록은 넣지 않음(API 명세 3절) */
public record ModelDetailResponse(Long id, String category, String name, String manufacturer, String purchaseYear,
	String purpose, RentalType rentalType, boolean multiDay, Integer maxRentalDays, Integer maxRentalMinutes,
	boolean carryOut, String location, String contact, Hours todayHours, int intervalMinutes, String guide,
	List<String> imageUrls, long normalUnits, long availableNow, ModelSource source) {

	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

	/** 오늘 운영 시간. 쉬는 날이면 todayHours 자체가 null */
	public record Hours(String open, String close) {
		static Hours of(RentalCalendar.Hours h) {
			return h == null ? null : new Hours(h.open().format(HM), h.close().format(HM));
		}
	}

	public static ModelDetailResponse of(EquipmentModel m, RentalCalendar.Hours today, List<String> imageUrls,
		long normalUnits, long availableNow) {
		return new ModelDetailResponse(m.getId(), m.getCategory(), m.getName(), m.getManufacturer(), m.getPurchaseYear(),
			m.getPurpose(), m.getRentalType(), m.isMultiDay(), m.getMaxRentalDays(), m.getMaxRentalMinutes(),
			m.isCarryOut(), m.getLocation(), m.getContact(), Hours.of(today), m.getIntervalMinutes(), m.getGuide(),
			imageUrls, normalUnits, availableNow, m.getSource());
	}
}
