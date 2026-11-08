package kr.ac.hansung.smartrent.domain.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;

/** 모델 추가·수정 요청(openapi ModelWrite). 시각은 HH:mm */
public record ModelWriteRequest(
	@NotBlank @Size(max = 20) String category,
	@NotBlank @Size(max = 100) String name,
	@Size(max = 50) String manufacturer,
	@Size(max = 4) String purchaseYear,
	@Size(max = 200) String purpose,
	@NotNull RentalType rentalType,
	@NotNull Boolean multiDay,
	@Positive Integer maxRentalDays,
	@Positive Integer maxRentalMinutes,
	@NotNull Boolean carryOut,
	@NotBlank @Size(max = 100) String location,
	@Size(max = 30) String contact,
	@NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String openTime,
	@NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String closeTime,
	@NotNull Integer intervalMinutes,
	String guide) {
}
