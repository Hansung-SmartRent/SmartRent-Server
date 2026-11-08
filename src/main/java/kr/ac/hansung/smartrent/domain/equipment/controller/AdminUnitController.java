package kr.ac.hansung.smartrent.domain.equipment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.dto.UnitPatchRequest;
import kr.ac.hansung.smartrent.domain.equipment.dto.UnitWriteRequest;
import kr.ac.hansung.smartrent.domain.equipment.service.AdminUnitService;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 기자재")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUnitController {

	private final AdminUnitService adminUnitService;

	@Operation(summary = "기기 추가")
	@PostMapping("/models/{id}/units")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminModelDetailResponse.Unit> add(@PathVariable Long id, @Valid @RequestBody UnitWriteRequest request) {
		return ApiResponse.ok(adminUnitService.add(id, request, CurrentUser.id()));
	}

	@Operation(summary = "기기 이름·상태 변경")
	@PatchMapping("/units/{id}")
	public ApiResponse<OperationChangeResponse> patch(@PathVariable Long id, @Valid @RequestBody UnitPatchRequest request) {
		return ApiResponse.ok(adminUnitService.patch(id, request, CurrentUser.id()));
	}

	@Operation(summary = "기기 삭제")
	@DeleteMapping("/units/{id}")
	public ApiResponse<OperationChangeResponse> delete(@PathVariable Long id) {
		return ApiResponse.ok(adminUnitService.delete(id, CurrentUser.id()));
	}
}
