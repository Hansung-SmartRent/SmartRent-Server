package kr.ac.hansung.smartrent.domain.operation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 임시 휴무 추가·공휴일 운영 해제 취소 요청(openapi HolidayWrite) */
public record HolidayWriteRequest(@NotNull LocalDate date, @NotBlank @Size(max = 50) String name) {
}
