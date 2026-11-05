package kr.ac.hansung.smartrent.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** openapi.yaml AnswerRequest: 관리자 답변 */
public record AnswerRequest(
	@NotBlank(message = "답변을 입력해 주세요.") @Size(max = 2000, message = "답변은 2000자 이하로 입력해 주세요.") String response) {
}
