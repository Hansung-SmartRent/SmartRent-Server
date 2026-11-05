package kr.ac.hansung.smartrent.domain.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 휴대폰이 사진으로 인식한 모델명(openapi.yaml RecognitionRequest) */
public record RecognitionRequest(
	@NotBlank(message = "모델명을 입력해 주세요.") @Size(max = 100, message = "모델명은 100자 이하로 입력해 주세요.") String modelName) {
}
