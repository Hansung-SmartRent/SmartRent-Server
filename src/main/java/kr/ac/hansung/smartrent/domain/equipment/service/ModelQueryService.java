package kr.ac.hansung.smartrent.domain.equipment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;

import kr.ac.hansung.smartrent.domain.equipment.dto.ModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.dto.ModelSummaryResponse;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.ModelImageRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalCalendar;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalSegment;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalSlotCalculator;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCalendarReader;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCapacityService;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기자재 목록·검색·상세(B1-10). 대수 계산은 대여 쪽(B2-01) 서비스를 그대로 부릅니다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelQueryService {

	private final EquipmentModelRepository modelRepository;
	private final EquipmentUnitRepository unitRepository;
	private final ModelImageRepository imageRepository;
	private final StorageService storageService;
	private final RentalCalendarReader calendarReader;
	private final RentalCapacityService capacityService;
	private final Clock clock;
	private final EntityManager em;
	private final RentalSlotCalculator slotCalculator = new RentalSlotCalculator();

	/** 운영 종료 모델은 빼고, 카테고리는 같은 값만, q는 이름·카테고리·제조사·활용 목적의 부분 문자열(대소문자 무시) */
	public List<ModelSummaryResponse> list(String category, String q, RentalType rentalType) {
		String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
		return modelRepository.findAllByArchivedFalseOrderByIdAsc().stream()
			.filter(m -> category == null || category.isBlank() || m.getCategory().equals(category.trim()))
			.filter(m -> rentalType == null || m.getRentalType() == rentalType)
			.filter(m -> needle == null || matches(m, needle))
			.map(this::summary)
			.toList();
	}

	/** 목록 한 줄(기기 코드 찾기·사진 인식 결과에도 씀) */
	public ModelSummaryResponse summary(EquipmentModel m) {
		return ModelSummaryResponse.of(m, thumbnail(m), normalUnits(m), availableNow(m, calendarReader.read(m)));
	}

	/** 사진 인식 결과(모델명)로 모델 하나 찾기(B1-12). 대소문자·공백 무시, 운영 종료 모델 제외, 없으면 404 */
	public ModelSummaryResponse recognize(String modelName) {
		return modelRepository.findByNameKeyAndArchivedFalse(EquipmentModel.nameKeyOf(modelName.trim()))
			.map(this::summary)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
	}

	/** 운영 종료 모델은 학생에게 보이지 않으므로 없는 것과 같이 404 */
	public ModelDetailResponse detail(Long id) {
		EquipmentModel model = modelRepository.findById(id).filter(m -> !m.isArchived())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		RentalCalendar calendar = calendarReader.read(model);
		LocalDate today = LocalDate.now(clock);
		List<String> urls = imageRepository.findAllByModelIdOrderBySortOrderAsc(model.getId()).stream()
			.map(i -> storageService.temporaryUrl(i.getImageKey())).toList();
		return ModelDetailResponse.of(model, calendar.isClosed(today) ? null : calendar.hours(today), urls,
			normalUnits(model), availableNow(model, calendar));
	}

	/** 관리자용 상세(B1-17): 운영 종료 모델도 보이고 개별 기기와 지금 대여 중인지까지 */
	public AdminModelDetailResponse adminDetail(Long id) {
		return adminDetail(modelRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
	}

	public AdminModelDetailResponse adminDetail(EquipmentModel model) {
		RentalCalendar calendar = calendarReader.read(model);
		LocalDate today = LocalDate.now(clock);
		List<String> urls = imageRepository.findAllByModelIdOrderBySortOrderAsc(model.getId()).stream()
			.map(i -> storageService.temporaryUrl(i.getImageKey())).toList();
		Set<Long> inUse = new HashSet<>(em.createQuery(
				"select r.unitId from Rental r where r.modelId = :id and r.status = :status and r.unitId is not null", Long.class)
			.setParameter("id", model.getId()).setParameter("status", RentalStatus.RENTING).getResultList());
		return AdminModelDetailResponse.of(model, calendar.isClosed(today) ? null : calendar.hours(today), urls,
			unitRepository.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(model.getId()), inUse,
			model.isArchived() ? 0 : availableNow(model, calendar));
	}

	/**
	 * 지금부터 다음 칸 끝까지 빌릴 수 있는 대수(API 명세 3절). 다음 칸 끝은 지금 이후 가장 가까운 칸의 끝으로,
	 * 현장 대여 반납 기한의 첫 선택지와 같습니다. 쉬는 날이거나 운영 시간 밖이면 0.
	 */
	public long availableNow(EquipmentModel model, RentalCalendar calendar) {
		LocalDateTime now = LocalDateTime.now(clock);
		return slotCalculator.slots(now.toLocalDate(), model.getIntervalMinutes(), calendar).stream()
			.filter(s -> !s.start().isAfter(now) && s.end().isAfter(now))
			.findFirst()
			.map(s -> (long) capacityService.available(model, new RentalSegment(now, s.end())))
			.orElse(0L);
	}

	private long normalUnits(EquipmentModel model) {
		return unitRepository.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(model.getId()).stream()
			.filter(u -> u.getCondition() == UnitCondition.NORMAL).count();
	}

	private String thumbnail(EquipmentModel model) {
		return imageRepository.findAllByModelIdOrderBySortOrderAsc(model.getId()).stream().findFirst()
			.map(i -> storageService.temporaryUrl(i.getImageKey())).orElse(null);
	}

	private static boolean matches(EquipmentModel m, String needle) {
		for (String field : new String[] {m.getName(), m.getCategory(), m.getManufacturer(), m.getPurpose()}) {
			if (field != null && field.toLowerCase(Locale.ROOT).contains(needle)) {
				return true;
			}
		}
		return false;
	}
}
