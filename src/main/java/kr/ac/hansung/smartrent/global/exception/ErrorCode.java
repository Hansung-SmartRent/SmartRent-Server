package kr.ac.hansung.smartrent.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 오류 코드 전체. docs/api/README.md 1-6절 "오류 코드 전체" 표와 이름·HTTP 상태·기본 메시지가 같아야 합니다.
 * 새 코드가 필요하면 명세부터 고친 뒤 여기에 추가합니다(ErrorCodeTest가 표와 비교).
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
	CONFLICT_RETRY(HttpStatus.CONFLICT, "다른 요청과 겹쳤습니다. 다시 시도해 주세요."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 뒤 다시 시도해 주세요."),
	EMAIL_DOMAIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "학교 이메일만 사용할 수 있습니다."),
	EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	OTP_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
	OTP_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 코드를 다시 받아 주세요."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일과 비밀번호를 확인해 주세요."),
	LOGIN_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "5분 동안 로그인이 제한됩니다."),
	STUDENT_NUMBER_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 학번입니다."),
	APPROVAL_NOT_PENDING(HttpStatus.CONFLICT, "승인 대기 중인 계정이 아닙니다."),
	WITHDRAW_BLOCKED_HELD(HttpStatus.CONFLICT, "보유 중인 기기를 먼저 반납해 주세요."),
	NOT_APPROVED(HttpStatus.FORBIDDEN, "승인된 학생 계정에서 예약할 수 있습니다."),
	SUSPENDED(HttpStatus.FORBIDDEN, "이용 정지 중입니다."),
	LATE_RENTAL_EXISTS(HttpStatus.CONFLICT, "연체 중인 기자재를 먼저 반납해 주세요."),
	SAME_KIND_ACTIVE(HttpStatus.CONFLICT, "같은 종류의 진행 중인 신청은 하나만 가능합니다."),
	MODEL_ARCHIVED(HttpStatus.CONFLICT, "운영이 종료된 모델입니다."),
	RENTAL_TOO_LONG(HttpStatus.BAD_REQUEST, "이 기자재는 한 번에 빌릴 수 있는 시간을 넘었습니다."),
	SEGMENT_INVALID(HttpStatus.BAD_REQUEST, "시작과 종료 시간을 확인해 주세요."),
	SEGMENTS_OVERLAP(HttpStatus.BAD_REQUEST, "예약 구간이 서로 겹칩니다."),
	OUT_OF_BOOKING_WINDOW(HttpStatus.BAD_REQUEST, "예약 가능 기간을 확인해 주세요."),
	CLOSED_DAY(HttpStatus.BAD_REQUEST, "운영하지 않는 날입니다."),
	RETURN_ON_CLOSED_DAY(HttpStatus.BAD_REQUEST, "반납일이 휴무일입니다. 다른 날을 선택해 주세요."),
	SLOT_INVALID(HttpStatus.BAD_REQUEST, "운영 시간 안의 시간 칸을 선택해 주세요."),
	SAME_DAY_ONLY(HttpStatus.BAD_REQUEST, "이 모델은 당일 대여만 가능합니다."),
	LONG_TERM_DATE_INVALID(HttpStatus.BAD_REQUEST, "장기 대여는 수령일만 고르고 반납은 6개월 뒤 운영일까지입니다."),
	NO_CAPACITY(HttpStatus.CONFLICT, "선택한 시간의 남은 수량이 없습니다."),
	NOT_CANCELLABLE(HttpStatus.CONFLICT, "시작 전 예약만 취소할 수 있습니다."),
	INVALID_STATUS(HttpStatus.CONFLICT, "지금 상태에서는 처리할 수 없습니다."),
	OUTSIDE_OPERATING_HOURS(HttpStatus.CONFLICT, "운영 시간에만 처리할 수 있습니다."),
	PROFESSOR_MAIL_REQUIRED(HttpStatus.BAD_REQUEST, "지도교수 승인 메일을 현장에서 확인해 주세요."),
	UNIT_MISMATCH(HttpStatus.BAD_REQUEST, "예약한 모델의 정상 기기인지 확인해 주세요."),
	UNIT_IN_USE(HttpStatus.CONFLICT, "이미 대여 중인 기기입니다."),
	NO_EARLY_CAPACITY(HttpStatus.CONFLICT, "조기 수령 가능한 수량이 없습니다."),
	RESERVATION_ENDED(HttpStatus.CONFLICT, "예약 시간이 종료되었습니다."),
	NO_SHOW_DEADLINE_PASSED(HttpStatus.CONFLICT, "수령 마감이 지나 노쇼로 처리되었습니다."),
	WALK_IN_END_INVALID(HttpStatus.BAD_REQUEST, "이 반납 기한으로는 빌려줄 수 없습니다."),
	EXTENSION_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 연장할 수 없습니다."),
	LONG_TERM_EXTENSION_BLOCKED(HttpStatus.CONFLICT, "장기 대여는 창구에 지도교수 승인 메일을 가져와 연장해 주세요."),
	EXTENSION_LIMIT(HttpStatus.BAD_REQUEST, "연장 가능 기한을 초과했습니다."),
	UNIT_DELETE_BLOCKED(HttpStatus.CONFLICT, "대여 중인 기기는 삭제할 수 없습니다."),
	UNIT_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 쓰는 기기 코드입니다."),
	MODEL_ARCHIVE_BLOCKED(HttpStatus.CONFLICT, "대여 중인 기기가 있어 운영을 종료할 수 없습니다."),
	MODEL_NAME_DUPLICATE(HttpStatus.CONFLICT, "같은 이름의 모델이 이미 있습니다."),
	WARNING_NOT_ACTIVE(HttpStatus.CONFLICT, "유효한 경고를 찾을 수 없습니다."),
	AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 추천을 잠시 사용할 수 없습니다."),
	FILE_INVALID(HttpStatus.BAD_REQUEST, "jpg 또는 png 사진을 올려 주세요."),
	MAIL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "메일을 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요."),
	STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "사진을 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요.");

	private final HttpStatus status;
	private final String message;
}
