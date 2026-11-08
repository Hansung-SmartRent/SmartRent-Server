package kr.ac.hansung.smartrent.domain.operation.service;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.operation.dto.HolidayResponse;
import kr.ac.hansung.smartrent.domain.operation.dto.HolidayWriteRequest;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import kr.ac.hansung.smartrent.domain.operation.entity.HolidaySource;
import kr.ac.hansung.smartrent.domain.operation.repository.HolidayRepository;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 휴무일 목록·임시 휴무·공휴일 운영(B1-19, 기획안 8장, DB 설계 3·4절 holidays).
 * 쉬는 날 = 토·일요일 또는 holidays에 있고 open_override=false인 날. 쉬는 날이 새로 생기면 저장한 뒤 같은 트랜잭션에서
 * B2-16 OperationChangeService.holidayAdded로 그날 걸친 예약을 취소하고 일주일 안의 반납 기한을 늘립니다.
 */
@Service
@RequiredArgsConstructor
public class HolidayService {

	private final HolidayRepository holidayRepository;
	private final OperationChangeService operationChangeService;
	private final AuditLogService auditLogService;
	private final EntityManager em;

	/** 양끝 포함. 주말은 넣지 않음(규칙으로 판단) */
	@Transactional(readOnly = true)
	public List<HolidayResponse> list(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "시작일이 종료일보다 뒤입니다.");
		}
		return holidayRepository.findAllByDateBetweenOrderByDateAsc(from, to).stream().map(HolidayResponse::of).toList();
	}

	/**
	 * 없는 날이면 임시 휴무(ADMIN)로 추가, 관리자 임시 휴무면 이름만 바꿈, 운영하기로 바꾼 공휴일이면 그 해제를 취소해 다시 쉬는 날로.
	 * 이미 쉬는 공휴일이면 바뀌는 것이 없어 빈 결과.
	 */
	@Transactional
	public OperationChangeResponse close(HolidayWriteRequest request, Long adminId) {
		LocalDate date = request.date();
		String name = request.name().trim();
		var existing = holidayRepository.findById(date);
		if (existing.isPresent() && existing.get().getSource() == HolidaySource.PUBLIC && !existing.get().isOpenOverride()) {
			return OperationChangeResponse.empty();
		}
		String detail;
		if (existing.isEmpty()) {
			holidayRepository.save(Holiday.builder().date(date).name(name).source(HolidaySource.ADMIN).openOverride(false).build());
			detail = "임시 휴무 추가 " + date + " " + name;
		}
		else if (existing.get().getSource() == HolidaySource.ADMIN) {
			existing.get().rename(name);
			detail = "임시 휴무 이름 변경 " + date + " " + name;
		}
		else {
			existing.get().closeAgain();
			detail = "공휴일 운영 해제 취소 " + date + " " + existing.get().getName();
		}
		em.flush();
		OperationChangeService.Result result = operationChangeService.holidayAdded(date);
		auditLogService.record("OPERATIONS", detail + ", 취소 예약 " + result.cancelled().size() + "건, 기한 연장 "
			+ result.extended().size() + "건", adminId);
		return OperationChangeResponse.of(result, em);
	}

	/** 공휴일만 운영일로 바꿈(open_override=true). 그날 공휴일이 없으면 404 */
	@Transactional
	public HolidayResponse open(LocalDate date, Long adminId) {
		Holiday holiday = holidayRepository.findById(date).filter(h -> h.getSource() == HolidaySource.PUBLIC)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		if (!holiday.isOpenOverride()) {
			holiday.openOnHoliday();
			auditLogService.record("OPERATIONS", "공휴일 운영 " + date + " " + holiday.getName(), adminId);
		}
		return HolidayResponse.of(holiday);
	}

	/** 관리자 임시 휴무는 지우고, 공휴일은 지우지 않고 운영하기로 바꿈. 그날 행이 없으면 404 */
	@Transactional
	public void delete(LocalDate date, Long adminId) {
		Holiday holiday = holidayRepository.findById(date).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		if (holiday.getSource() == HolidaySource.ADMIN) {
			holidayRepository.delete(holiday);
			auditLogService.record("OPERATIONS", "임시 휴무 삭제 " + date + " " + holiday.getName(), adminId);
			return;
		}
		open(date, adminId);
	}
}
