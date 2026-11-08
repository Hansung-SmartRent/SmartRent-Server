package kr.ac.hansung.smartrent.domain.equipment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.equipment.dto.DamageReportRequest;
import kr.ac.hansung.smartrent.domain.equipment.service.DamageReportService;
import kr.ac.hansung.smartrent.domain.rental.dto.RentalResponse;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대여")
@RestController
@RequiredArgsConstructor
public class DamageReportController {

	private final DamageReportService damageReportService;

	@Operation(summary = "기기 이상 신고")
	@PostMapping("/api/v1/rentals/{id}/damage-report")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<RentalResponse> report(@PathVariable Long id, @Valid @RequestBody DamageReportRequest request) {
		return ApiResponse.ok(damageReportService.report(id, CurrentUser.id(), request.note()));
	}
}
