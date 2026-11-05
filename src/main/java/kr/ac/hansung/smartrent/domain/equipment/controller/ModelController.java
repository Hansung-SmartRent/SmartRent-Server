package kr.ac.hansung.smartrent.domain.equipment.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.hansung.smartrent.domain.equipment.dto.ModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.dto.ModelSummaryResponse;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.service.ModelQueryService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자 누구나(SecurityConfig). 작은 목록이라 쪽을 나누지 않고 배열로 줌(API 명세 1-4절) */
@Tag(name = "기자재")
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

	private final ModelQueryService modelQueryService;

	@Operation(summary = "기자재 목록·검색")
	@GetMapping
	public ApiResponse<List<ModelSummaryResponse>> list(@RequestParam(required = false) String category,
		@RequestParam(required = false) String q, @RequestParam(required = false) RentalType rentalType) {
		return ApiResponse.ok(modelQueryService.list(category, q, rentalType));
	}

	@Operation(summary = "기자재 상세")
	@GetMapping("/{id}")
	public ApiResponse<ModelDetailResponse> detail(@PathVariable Long id) {
		return ApiResponse.ok(modelQueryService.detail(id));
	}
}
