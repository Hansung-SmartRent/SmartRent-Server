package kr.ac.hansung.smartrent.global.storage;

import java.time.Clock;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.global.port.OperationsAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 파일 삭제 작업(DB 설계 3절 storage_deletion_jobs, B1-08 소유).
 * 1) 부르는 쪽 트랜잭션 안에서 지울 키를 표에 남기고 2) 커밋 직후 바로 지워 봄 3) 실패하면 행을 두고 1분마다 다시(StorageDeletionScheduler).
 * 성공하거나 이미 없는 파일이면 행을 지웁니다. 첫 실패 때 관리자에게 OPERATIONS 알림을 남깁니다. 키·사진 내용은 로그와 알림에 쓰지 않습니다.
 */
@Slf4j
@Service
public class StorageDeletionService {

	private final StorageDeletionJobRepository repository;
	private final StorageService storageService;
	private final OperationsAlert operationsAlert;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public StorageDeletionService(StorageDeletionJobRepository repository, StorageService storageService,
		OperationsAlert operationsAlert, PlatformTransactionManager transactionManager, Clock clock) {
		this.repository = repository;
		this.storageService = storageService;
		this.operationsAlert = operationsAlert;
		// 커밋 직후(afterCommit)에도 부르므로 항상 새 트랜잭션으로 기록
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.clock = clock;
	}

	/** 부르는 쪽 트랜잭션과 함께 저장되고, 커밋된 뒤에 지워 봅니다. 트랜잭션 밖이면 바로 지워 봅니다 */
	public void request(String key) {
		StorageDeletionJob job = repository.save(new StorageDeletionJob(storageService.mode(), key));
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			Long id = job.getId();
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					attempt(id);
				}
			});
		}
		else {
			attempt(job.getId());
		}
	}

	/** 남은 작업을 모두 다시 시도(1분마다, 서버를 켤 때도) */
	public void retryAll() {
		for (StorageDeletionJob job : repository.findAllByStorageModeOrderByIdAsc(storageService.mode())) {
			attempt(job.getId());
		}
	}

	void attempt(Long jobId) {
		StorageDeletionJob job = transactionTemplate.execute(status -> repository.findById(jobId).map(found -> {
			found.recordAttempt(LocalDateTime.now(clock));
			return found;
		}).orElse(null));
		if (job == null) {
			return;
		}
		try {
			storageService.delete(job.getObjectKey());
		}
		catch (RuntimeException e) {
			log.warn("파일 삭제 실패, 1분 뒤 다시 시도합니다(작업 {} / {}번째)", job.getId(), job.getAttemptCount());
			if (job.getAttemptCount() == 1) {
				operationsAlert.send("사진 삭제 실패", "저장소에서 사진을 지우지 못해 1분마다 다시 시도합니다(작업 " + job.getId() + ").");
			}
			return;
		}
		transactionTemplate.executeWithoutResult(status -> repository.deleteById(jobId));
	}
}
