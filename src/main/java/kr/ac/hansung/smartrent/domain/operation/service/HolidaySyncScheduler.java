package kr.ac.hansung.smartrent.domain.operation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * HOLIDAY_SYNC_ENABLED=true일 때만 공휴일을 받습니다(외부 연결 4절).
 * 매일 새벽 4시(한국 시각)와 서버가 켜진 직후에 받습니다. prod는 처음 데이터에 공휴일이 없어 켜자마자 채웁니다.
 * 받기에 실패해도 서버는 켜지고 기존 달력을 그대로 씁니다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "smartrent.holiday.sync-enabled", havingValue = "true")
public class HolidaySyncScheduler {

	private final HolidaySyncService holidaySyncService;

	@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
	public void run() {
		holidaySyncService.sync();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStart() {
		holidaySyncService.sync();
	}
}
