package kr.ac.hansung.smartrent.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** openapi.yaml RejectRequest: 학생에게 보여 줄 반려 사유 */
public record RejectRequest(
	@NotBlank(message = "반려 사유를 입력해 주세요.") @Size(max = 200, message = "반려 사유는 200자 이하로 입력해 주세요.") String reason) {
}
