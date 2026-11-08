package kr.ac.hansung.smartrent.domain.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 기기 이상 신고 요청(openapi DamageReport) */
public record DamageReportRequest(@NotBlank @Size(max = 500) String note) {
}
