package kr.ac.hansung.smartrent.domain.user.dto;

import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.entity.User;

/** openapi.yaml Me: 로그인 응답과 내 정보 조회에 같이 씀. 앱은 approval·suspension으로 보여 줄 화면을 고름 */
public record MeResponse(Long id, Role role, String email, String name, String studentNumber, Approval approval,
	String rejectReason, String profileImageUrl, long activeWarningCount, Suspension suspension) {

	public record Suspension(boolean suspended, LocalDateTime until, boolean untilReturn) {
	}

	/** profileImageUrl은 10분짜리 임시 주소(사진이 없으면 null). 만드는 곳은 MeAssembler */
	public static MeResponse of(User user, String profileImageUrl, long activeWarningCount, LocalDateTime now) {
		return new MeResponse(user.getId(), user.getRole(), user.getEmail(), user.getName(), user.getStudentNumber(),
			user.getApproval(), user.getRejectReason(), profileImageUrl, activeWarningCount,
			new Suspension(user.isSuspended(now), user.getSuspendedUntil(), user.isSuspendedUntilReturn()));
	}
}
