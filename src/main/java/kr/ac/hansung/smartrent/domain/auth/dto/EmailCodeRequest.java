package kr.ac.hansung.smartrent.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.ac.hansung.smartrent.domain.auth.entity.VerificationPurpose;

public record EmailCodeRequest(
	@NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식을 확인해 주세요.") String email,
	@NotNull(message = "필수 값입니다.") VerificationPurpose purpose) {
}
