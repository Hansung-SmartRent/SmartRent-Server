package kr.ac.hansung.smartrent.domain.equipment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.dto.ModelWriteRequest;
import kr.ac.hansung.smartrent.domain.equipment.service.AdminModelService;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 기자재")
@RestController
@RequestMapping("/api/v1/admin/models")
@RequiredArgsConstructor
public class AdminModelController {

	private final AdminModelService adminModelService;

	@Operation(summary = "관리자용 모델 상세")
	@GetMapping("/{id}")
	public ApiResponse<AdminModelDetailResponse> detail(@PathVariable Long id) {
		return ApiResponse.ok(adminModelService.detail(id));
	}

	@Operation(summary = "모델 추가")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminModelDetailResponse> create(@Valid @RequestBody ModelWriteRequest request) {
		return ApiResponse.ok(adminModelService.create(request, CurrentUser.id()));
	}

	@Operation(summary = "모델 수정")
	@PutMapping("/{id}")
	public ApiResponse<OperationChangeResponse> update(@PathVariable Long id, @Valid @RequestBody ModelWriteRequest request) {
		return ApiResponse.ok(adminModelService.update(id, request, CurrentUser.id()));
	}

	@Operation(summary = "모델 운영 종료")
	@PostMapping("/{id}/archive")
	public ApiResponse<OperationChangeResponse> archive(@PathVariable Long id) {
		return ApiResponse.ok(adminModelService.archive(id, CurrentUser.id()));
	}
}
