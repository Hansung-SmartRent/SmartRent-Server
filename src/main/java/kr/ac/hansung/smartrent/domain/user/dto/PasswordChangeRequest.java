package kr.ac.hansung.smartrent.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** code: 현재 이메일로 받은 인증번호(PASSWORD_RESET 목적). 현재 비밀번호는 받지 않음(기획안 1장) */
public record PasswordChangeRequest(
	@NotBlank(message = "인증번호를 입력해 주세요.") @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.") String code,
	@NotBlank(message = "비밀번호를 입력해 주세요.") @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해 주세요.") String newPassword) {
}
