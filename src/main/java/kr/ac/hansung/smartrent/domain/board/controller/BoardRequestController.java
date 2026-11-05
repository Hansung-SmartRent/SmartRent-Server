package kr.ac.hansung.smartrent.domain.board.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.ac.hansung.smartrent.domain.board.dto.AnswerRequest;
import kr.ac.hansung.smartrent.domain.board.dto.RequestItemResponse;
import kr.ac.hansung.smartrent.domain.board.dto.RequestWriteRequest;
import kr.ac.hansung.smartrent.domain.board.entity.RequestKind;
import kr.ac.hansung.smartrent.domain.board.service.BoardRequestService;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 문의·구매 요청. /admin 아래는 관리자(SecurityConfig), 쓰기·내 목록은 학생(서비스에서 확인) */
@Tag(name = "문의·구매 요청")
@Validated
@RestController
@RequiredArgsConstructor
public class BoardRequestController {

	private final BoardRequestService service;

	@Operation(summary = "문의 쓰기")
	@PostMapping("/api/v1/inquiries")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RequestItemResponse> writeInquiry(@Valid @RequestBody RequestWriteRequest request) {
		return ApiResponse.ok(service.write(RequestKind.INQUIRY, CurrentUser.id(), request.title(), request.body()));
	}

	@Operation(summary = "내 문의")
	@GetMapping("/api/v1/inquiries/me")
	public ApiResponse<List<RequestItemResponse>> myInquiries() {
		return ApiResponse.ok(service.mine(RequestKind.INQUIRY, CurrentUser.id()));
	}

	@Operation(summary = "문의 목록")
	@GetMapping("/api/v1/admin/inquiries")
	public ApiResponse<PageResponse<RequestItemResponse>> inquiries(@RequestParam(required = false) Boolean answered,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~100입니다.") @Max(value = 100, message = "size는 1~100입니다.") int size) {
		return ApiResponse.ok(service.list(RequestKind.INQUIRY, answered, page, size));
	}

	@Operation(summary = "문의 답변")
	@PostMapping("/api/v1/admin/inquiries/{id}/answer")
	public ApiResponse<RequestItemResponse> answerInquiry(@PathVariable Long id, @Valid @RequestBody AnswerRequest request) {
		return ApiResponse.ok(service.answer(RequestKind.INQUIRY, id, request.response()));
	}

	@Operation(summary = "구매 요청 쓰기")
	@PostMapping("/api/v1/purchase-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RequestItemResponse> writePurchase(@Valid @RequestBody RequestWriteRequest request) {
		return ApiResponse.ok(service.write(RequestKind.PURCHASE_REQUEST, CurrentUser.id(), request.title(), request.body()));
	}

	@Operation(summary = "내 구매 요청")
	@GetMapping("/api/v1/purchase-requests/me")
	public ApiResponse<List<RequestItemResponse>> myPurchases() {
		return ApiResponse.ok(service.mine(RequestKind.PURCHASE_REQUEST, CurrentUser.id()));
	}

	@Operation(summary = "구매 요청 목록")
	@GetMapping("/api/v1/admin/purchase-requests")
	public ApiResponse<PageResponse<RequestItemResponse>> purchases(@RequestParam(required = false) Boolean answered,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~100입니다.") @Max(value = 100, message = "size는 1~100입니다.") int size) {
		return ApiResponse.ok(service.list(RequestKind.PURCHASE_REQUEST, answered, page, size));
	}

	@Operation(summary = "구매 요청 답변")
	@PostMapping("/api/v1/admin/purchase-requests/{id}/answer")
	public ApiResponse<RequestItemResponse> answerPurchase(@PathVariable Long id, @Valid @RequestBody AnswerRequest request) {
		return ApiResponse.ok(service.answer(RequestKind.PURCHASE_REQUEST, id, request.response()));
	}
}
