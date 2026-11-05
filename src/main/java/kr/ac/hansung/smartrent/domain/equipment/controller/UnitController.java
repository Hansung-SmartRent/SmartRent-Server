package kr.ac.hansung.smartrent.domain.equipment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.hansung.smartrent.domain.equipment.dto.ScanResultResponse;
import kr.ac.hansung.smartrent.domain.equipment.service.UnitLookupService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자 누구나(SecurityConfig) */
@Tag(name = "기자재")
@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class UnitController {

	private final UnitLookupService unitLookupService;

	@Operation(summary = "기기 코드로 찾기")
	@GetMapping("/by-code/{code}")
	public ApiResponse<ScanResultResponse> byCode(@PathVariable String code) {
		return ApiResponse.ok(unitLookupService.byCode(code, CurrentUser.id()));
	}
}
