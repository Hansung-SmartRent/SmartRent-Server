package kr.ac.hansung.smartrent.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** openapi.yaml StudentPatch: 관리자가 고칠 이름·학번 */
public record StudentPatchRequest(
	@NotBlank(message = "이름을 입력해 주세요.") @Size(max = 30, message = "이름은 30자 이하로 입력해 주세요.") String name,
	@NotBlank(message = "학번을 입력해 주세요.") @Pattern(regexp = "^[0-9]{7,10}$", message = "학번은 숫자 7~10자리로 입력해 주세요.") String studentNumber) {
}
