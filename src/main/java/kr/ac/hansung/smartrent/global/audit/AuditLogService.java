package kr.ac.hansung.smartrent.global.audit;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리 기록 남기기. 두 사람이 각자 기능에서 부릅니다(DB 설계 2절 audit_logs 공동).
 * 부르는 쪽 처리와 같은 트랜잭션에서 저장되어, 처리가 취소되면 기록도 남지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	/**
	 * @param action  처리 종류(대문자, 예: PICKUP, APPROVE_STUDENT)
	 * @param detail  사람이 읽는 내용(500자 이하). 비밀번호·인증번호·사진 키는 넣지 않습니다
	 * @param actorId 처리한 관리자 id. 판정 작업 같은 서버 자동 처리는 null
	 */
	@Transactional
	public AuditLog record(String action, String detail, Long actorId) {
		return record(action, detail, actorId, null);
	}

	/** retainUntil: 탈퇴 학생의 학번이 든 기록처럼 기한 뒤에 내용을 바꿔야 하는 기록(DB 설계 7절 보관 삭제) */
	@Transactional
	public AuditLog record(String action, String detail, Long actorId, LocalDateTime retainUntil) {
		return auditLogRepository.save(AuditLog.builder()
			.action(action)
			.detail(detail)
			.actorId(actorId)
			.retainUntil(retainUntil)
			.build());
	}
}
