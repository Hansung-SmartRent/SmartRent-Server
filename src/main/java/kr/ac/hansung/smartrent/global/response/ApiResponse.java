package kr.ac.hansung.smartrent.global.response;

import kr.ac.hansung.smartrent.global.exception.ErrorCode;

/**
 * 모든 응답의 공통 틀(API 명세 1-2절).
 * 성공: { "success": true, "message": "...", "data": {...}, "errorCode": null }
 * 실패: { "success": false, "message": "...", "data": null, "errorCode": "NOT_FOUND" }
 */
public record ApiResponse<T>(boolean success, String message, T data, String errorCode) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, null, data, null);
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data, null);
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode) {
		return new ApiResponse<>(false, errorCode.getMessage(), null, errorCode.name());
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, message, null, errorCode.name());
	}

	public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
		return new ApiResponse<>(false, errorCode.getMessage(), data, errorCode.name());
	}
}
