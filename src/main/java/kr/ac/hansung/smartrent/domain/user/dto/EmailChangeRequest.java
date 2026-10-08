package kr.ac.hansung.smartrent.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** code: 새 이메일로 받은 인증번호(EMAIL_CHANGE 목적) */
public record EmailChangeRequest(
	@NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식을 확인해 주세요.") String newEmail,
	@NotBlank(message = "인증번호를 입력해 주세요.") @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.") String code) {
}
