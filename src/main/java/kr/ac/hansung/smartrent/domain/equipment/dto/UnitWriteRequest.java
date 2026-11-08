package kr.ac.hansung.smartrent.domain.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 기기 추가 요청(openapi UnitWrite) */
public record UnitWriteRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 100) String name) {
}
