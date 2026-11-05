package kr.ac.hansung.smartrent.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** openapi.yaml RequestWrite: 문의·구매 요청 쓰기 */
public record RequestWriteRequest(
	@NotBlank(message = "제목을 입력해 주세요.") @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.") String title,
	@NotBlank(message = "내용을 입력해 주세요.") @Size(max = 2000, message = "내용은 2000자 이하로 입력해 주세요.") String body) {
}
