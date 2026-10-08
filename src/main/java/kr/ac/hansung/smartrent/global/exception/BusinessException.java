package kr.ac.hansung.smartrent.global.exception;

import lombok.Getter;

/** 업무 규칙에 걸려 요청을 거절할 때 던집니다. 예: throw new BusinessException(ErrorCode.NO_CAPACITY) */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	/** 기본 메시지 대신 상황에 맞는 문장을 보여 줄 때 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
