package kr.ac.hansung.smartrent.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.ac.hansung.smartrent.domain.user.dto.AdminStudentDetailResponse;
import kr.ac.hansung.smartrent.domain.user.dto.AdminStudentResponse;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.domain.user.dto.StudentPatchRequest;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.service.AdminStudentService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 권한은 SecurityConfig(/api/v1/admin/**)에서 확인 */
@Tag(name = "관리자 학생")
@Validated
@RestController
@RequestMapping("/api/v1/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

	private final AdminStudentService adminStudentService;

	@Operation(summary = "학생 목록")
	@GetMapping
	public ApiResponse<PageResponse<AdminStudentResponse>> list(@RequestParam(required = false) Approval approval,
		@RequestParam(required = false) String q, @RequestParam(required = false) Boolean hasWarnings,
		@RequestParam(required = false) Boolean suspended,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~100입니다.") @Max(value = 100, message = "size는 1~100입니다.") int size) {
		return ApiResponse.ok(adminStudentService.list(approval, q, hasWarnings, suspended, page, size));
	}

	@Operation(summary = "학생 상세")
	@GetMapping("/{id}")
	public ApiResponse<AdminStudentDetailResponse> detail(@PathVariable Long id) {
		return ApiResponse.ok(adminStudentService.detail(id));
	}

	@Operation(summary = "학생 이름·학번 수정")
	@PatchMapping("/{id}")
	public ApiResponse<AdminStudentResponse> correct(@PathVariable Long id, @Valid @RequestBody StudentPatchRequest request) {
		return ApiResponse.ok(adminStudentService.correct(id, request.name(), request.studentNumber(), CurrentUser.id()));
	}
}
