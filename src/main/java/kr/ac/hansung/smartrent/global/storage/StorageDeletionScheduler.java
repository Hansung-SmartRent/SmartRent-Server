package kr.ac.hansung.smartrent.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 남은 파일 삭제 작업을 1분마다(확정) 다시 시도. 서버를 켠 직후에도 한 번 돕니다. 시험에서는 끔 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "smartrent.storage.retry-enabled", havingValue = "true", matchIfMissing = true)
public class StorageDeletionScheduler {

	private final StorageDeletionService deletionService;

	@Scheduled(initialDelayString = "PT10S", fixedDelayString = "PT1M")
	public void retry() {
		deletionService.retryAll();
	}
}
