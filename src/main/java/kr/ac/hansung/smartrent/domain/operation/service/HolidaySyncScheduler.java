package kr.ac.hansung.smartrent.domain.operation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** HOLIDAY_SYNC_ENABLED=true일 때만 매일 새벽 4시(한국 시각)에 공휴일을 받습니다(외부 연결 4절) */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "smartrent.holiday.sync-enabled", havingValue = "true")
public class HolidaySyncScheduler {

	private final HolidaySyncService holidaySyncService;

	@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
	public void run() {
		holidaySyncService.sync();
	}
}
