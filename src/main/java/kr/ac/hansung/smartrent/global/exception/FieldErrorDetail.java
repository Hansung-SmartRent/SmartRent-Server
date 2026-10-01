package kr.ac.hansung.smartrent.global.exception;

/** VALIDATION_ERROR 응답의 data에 들어가는 칸별 오류(API 명세 1-7절) */
public record FieldErrorDetail(String field, String reason) {
}
