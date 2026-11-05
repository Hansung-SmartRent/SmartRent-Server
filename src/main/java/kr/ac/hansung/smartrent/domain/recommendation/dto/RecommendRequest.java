package kr.ac.hansung.smartrent.domain.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** openapi.yaml RecommendRequest. 사용 목적 1~200자 */
public record RecommendRequest(
	@NotBlank(message = "사용 목적을 입력해 주세요.") @Size(max = 200, message = "사용 목적은 200자 이하로 입력해 주세요.") String purpose) {
}
