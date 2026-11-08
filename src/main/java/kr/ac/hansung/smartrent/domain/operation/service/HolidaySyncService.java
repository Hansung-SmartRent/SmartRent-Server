package kr.ac.hansung.smartrent.domain.operation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import kr.ac.hansung.smartrent.domain.operation.client.HolidayClient;
import kr.ac.hansung.smartrent.domain.operation.client.HolidayClient.PublicHoliday;
import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import kr.ac.hansung.smartrent.domain.operation.entity.HolidaySource;
import kr.ac.hansung.smartrent.domain.operation.repository.HolidayRepository;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.global.port.OperationsAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 공휴일 받기(B1-19, 기획안 8장, 외부 연결 4절). 올해와 내년을 받아
 * 새 날짜는 source=PUBLIC으로 넣고 그날 걸린 예약을 B2-16 서비스로 임시 휴무와 같이 정리, 이미 있는 공휴일은 이름만 맞춥니다.
 * 관리자가 넣은 임시 휴무(ADMIN)와 공휴일 운영 표시(open_override)는 건드리지 않습니다.
 * 한 해라도 받지 못하면 아무것도 바꾸지 않고(기존 달력 유지) 관리자에게 OPERATIONS 알림을 남깁니다. 다음 날 같은 시각에 다시 합니다.
 */
@Slf4j
@Service
public class HolidaySyncService {

	private final HolidayClient client;
	private final HolidayRepository holidayRepository;
	private final OperationChangeService operationChangeService;
	private final OperationsAlert operationsAlert;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public HolidaySyncService(HolidayClient client, HolidayRepository holidayRepository,
		OperationChangeService operationChangeService, OperationsAlert operationsAlert,
		PlatformTransactionManager transactionManager, Clock clock) {
		this.client = client;
		this.holidayRepository = holidayRepository;
		this.operationChangeService = operationChangeService;
		this.operationsAlert = operationsAlert;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.clock = clock;
	}

	/** @return 새로 넣은 공휴일 수. 실패하면 -1 */
	public int sync() {
		int year = LocalDate.now(clock).getYear();
		List<PublicHoliday> fetched = new ArrayList<>();
		try {
			fetched.addAll(client.fetch(year));
			fetched.addAll(client.fetch(year + 1));
		}
		catch (RuntimeException e) {
			log.warn("공휴일 받기 실패, 기존 달력을 유지합니다: {}", e.getMessage());
			operationsAlert.send("공휴일 받기 실패", "공휴일을 받지 못해 기존 달력을 그대로 씁니다. 내일 새벽 4시에 다시 시도합니다.");
			return -1;
		}
		Integer added = transactionTemplate.execute(status -> apply(fetched));
		log.info("공휴일 받기 완료: {}일 받음, 새로 {}일", fetched.size(), added);
		return added == null ? 0 : added;
	}

	private int apply(List<PublicHoliday> fetched) {
		int added = 0;
		for (PublicHoliday holiday : fetched) {
			var existing = holidayRepository.findById(holiday.date());
			if (existing.isPresent()) {
				Holiday saved = existing.get();
				if (saved.getSource() == HolidaySource.PUBLIC && !saved.getName().equals(holiday.name())) {
					saved.rename(holiday.name());
				}
				continue;
			}
			holidayRepository.saveAndFlush(Holiday.builder().date(holiday.date()).name(holiday.name())
				.source(HolidaySource.PUBLIC).openOverride(false).build());
			operationChangeService.holidayAdded(holiday.date());
			added++;
		}
		return added;
	}
}
