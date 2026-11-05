package kr.ac.hansung.smartrent.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import kr.ac.hansung.smartrent.domain.user.entity.Approval;

/** openapi.yaml AdminStudentDetail = AdminStudent + 학생증 사진 임시 주소(승인 대기 중일 때만) + 경고 + 대여 기록 */
public record AdminStudentDetailResponse(Long id, String email, String name, String studentNumber, Approval approval,
	boolean hasStudentIdImage, long activeWarningCount, MeResponse.Suspension suspension, LocalDateTime createdAt,
	String studentIdImageUrl, List<?> warnings, List<?> rentals) {

	public static AdminStudentDetailResponse of(AdminStudentResponse s, String studentIdImageUrl, List<?> warnings,
		List<?> rentals) {
		return new AdminStudentDetailResponse(s.id(), s.email(), s.name(), s.studentNumber(), s.approval(),
			s.hasStudentIdImage(), s.activeWarningCount(), s.suspension(), s.createdAt(), studentIdImageUrl, warnings, rentals);
	}
}
