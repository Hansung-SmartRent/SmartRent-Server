package kr.ac.hansung.smartrent.domain.user.dto;

import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.User;

/** openapi.yaml AdminStudent: 관리자 학생 목록 한 줄 */
public record AdminStudentResponse(Long id, String email, String name, String studentNumber, Approval approval,
	boolean hasStudentIdImage, long activeWarningCount, MeResponse.Suspension suspension, LocalDateTime createdAt) {

	public static AdminStudentResponse of(User u, long activeWarningCount, LocalDateTime now) {
		return new AdminStudentResponse(u.getId(), u.getEmail(), u.getName(), u.getStudentNumber(), u.getApproval(),
			u.getStudentIdImageKey() != null, activeWarningCount,
			new MeResponse.Suspension(u.isSuspended(now), u.getSuspendedUntil(), u.isSuspendedUntilReturn()), u.getCreatedAt());
	}
}
