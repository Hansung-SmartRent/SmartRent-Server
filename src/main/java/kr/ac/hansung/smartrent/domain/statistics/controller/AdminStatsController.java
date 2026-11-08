package kr.ac.hansung.smartrent.domain.statistics.controller;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.ac.hansung.smartrent.domain.statistics.dto.AuditLogResponse;
import kr.ac.hansung.smartrent.domain.statistics.dto.StatsResponse;
import kr.ac.hansung.smartrent.domain.statistics.service.AuditLogQueryService;
import kr.ac.hansung.smartrent.domain.statistics.service.StatsService;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통계·기록")
@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatsController {

	private final StatsService statsService;
	private final AuditLogQueryService auditLogQueryService;

	@Operation(summary = "이용 통계")
	@GetMapping("/stats")
	public ApiResponse<StatsResponse> stats(@RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.ok(statsService.stats(from, to));
	}

	@Operation(summary = "관리 기록")
	@GetMapping("/audit-logs")
	public ApiResponse<PageResponse<AuditLogResponse>> auditLogs(
		@RequestParam(required = false) String action,
		@RequestParam(required = false) List<String> actions,
		@RequestParam(required = false) LocalDate from,
		@RequestParam(required = false) LocalDate to,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~100입니다.") @Max(value = 100, message = "size는 1~100입니다.") int size) {
		return ApiResponse.ok(auditLogQueryService.list(action, actions, from, to, page, size));
	}
}
