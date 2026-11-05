package kr.ac.hansung.smartrent.domain.board.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.ac.hansung.smartrent.domain.board.dto.NoticeResponse;
import kr.ac.hansung.smartrent.domain.board.dto.NoticeWriteRequest;
import kr.ac.hansung.smartrent.domain.board.service.NoticeService;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 목록은 로그인한 사용자, /admin 아래는 관리자(SecurityConfig) */
@Tag(name = "공지")
@Validated
@RestController
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeService noticeService;

	@Operation(summary = "공지 목록")
	@GetMapping("/api/v1/notices")
	public ApiResponse<PageResponse<NoticeResponse>> list(
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~100입니다.") @Max(value = 100, message = "size는 1~100입니다.") int size) {
		return ApiResponse.ok(noticeService.list(page, size));
	}

	@Operation(summary = "공지 쓰기")
	@PostMapping("/api/v1/admin/notices")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<NoticeResponse> write(@Valid @RequestBody NoticeWriteRequest request) {
		return ApiResponse.ok(noticeService.write(request.title(), request.body(), CurrentUser.id()));
	}

	@Operation(summary = "공지 수정")
	@PutMapping("/api/v1/admin/notices/{id}")
	public ApiResponse<NoticeResponse> edit(@PathVariable Long id, @Valid @RequestBody NoticeWriteRequest request) {
		return ApiResponse.ok(noticeService.edit(id, request.title(), request.body()));
	}

	@Operation(summary = "공지 삭제")
	@DeleteMapping("/api/v1/admin/notices/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		noticeService.delete(id);
		return ApiResponse.ok(null);
	}
}
