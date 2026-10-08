package kr.ac.hansung.smartrent.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
	@NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식을 확인해 주세요.") String email,
	@NotBlank(message = "인증번호를 입력해 주세요.") @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.") String code,
	@NotBlank(message = "비밀번호를 입력해 주세요.") @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해 주세요.") String newPassword) {
}
