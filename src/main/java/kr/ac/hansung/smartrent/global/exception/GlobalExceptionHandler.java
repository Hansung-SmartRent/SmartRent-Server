package kr.ac.hansung.smartrent.global.exception;

import java.util.List;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 예외를 공통 응답 틀과 ErrorCode의 HTTP 상태로 바꿉니다(API 명세 1-6절). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code, e.getMessage()));
	}

	/** @Valid 요청 본문 검사 실패: data에 칸 이름별 오류 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleNotValid(MethodArgumentNotValidException e) {
		List<FieldErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
			.map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
			.toList();
		return validationError(errors);
	}

	/** 주소·쿼리 값 검사 실패(@Validated) */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleConstraint(ConstraintViolationException e) {
		List<FieldErrorDetail> errors = e.getConstraintViolations().stream()
			.map(v -> new FieldErrorDetail(lastNode(v.getPropertyPath().toString()), v.getMessage()))
			.toList();
		return validationError(errors);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleMissingParam(MissingServletRequestParameterException e) {
		return validationError(List.of(new FieldErrorDetail(e.getParameterName(), "필수 값입니다.")));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		return validationError(List.of(new FieldErrorDetail(e.getName(), "형식이 올바르지 않습니다.")));
	}

	/** JSON 형식 오류, 날짜·enum 값 오류 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
		ErrorCode code = ErrorCode.VALIDATION_ERROR;
		return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code));
	}

	/** 없는 주소 */
	@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
	public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
		ErrorCode code = ErrorCode.NOT_FOUND;
		return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code));
	}

	/**
	 * 위에서 처리하지 못한 예외: 500 INTERNAL_ERROR. 원인은 로그에만 남기고 응답에는 넣지 않습니다.
	 * 405처럼 Spring이 상태 코드를 정해 둔 요청 오류는 500으로 바꾸지 않고 Spring 기본 처리에 맡깁니다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) throws Exception {
		if (e instanceof ErrorResponse) {
			throw e;
		}
		log.error("처리하지 못한 예외", e);
		ErrorCode code = ErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code));
	}

	private ResponseEntity<ApiResponse<List<FieldErrorDetail>>> validationError(List<FieldErrorDetail> errors) {
		ErrorCode code = ErrorCode.VALIDATION_ERROR;
		return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code, errors));
	}

	private static String lastNode(String path) {
		int dot = path.lastIndexOf('.');
		return dot < 0 ? path : path.substring(dot + 1);
	}
}
