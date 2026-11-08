package kr.ac.hansung.smartrent.domain.operation.controller;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.operation.dto.HoursItemResponse;
import kr.ac.hansung.smartrent.domain.operation.dto.HoursWriteRequest;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.domain.operation.service.OperatingHoursService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영")
@RestController
@RequestMapping("/api/v1/admin/models/{id}/hours")
@RequiredArgsConstructor
public class AdminHoursController {

	private final OperatingHoursService operatingHoursService;

	@Operation(summary = "운영 시간 변경 목록")
	@GetMapping
	public ApiResponse<List<HoursItemResponse>> list(@PathVariable Long id) {
		return ApiResponse.ok(operatingHoursService.list(id));
	}

	@Operation(summary = "운영 시간 바꾸기")
	@PutMapping
	public ApiResponse<OperationChangeResponse> change(@PathVariable Long id, @Valid @RequestBody HoursWriteRequest request) {
		return ApiResponse.ok(operatingHoursService.change(id, request, CurrentUser.id()));
	}

	@Operation(summary = "날짜별 운영 시간 되돌리기")
	@DeleteMapping("/{date}")
	public ApiResponse<OperationChangeResponse> revert(@PathVariable Long id, @PathVariable LocalDate date) {
		return ApiResponse.ok(operatingHoursService.revert(id, date, CurrentUser.id()));
	}
}
