package kr.ac.hansung.smartrent.domain.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.recommendation.dto.RecommendRequest;
import kr.ac.hansung.smartrent.domain.recommendation.dto.RecommendationResponse;
import kr.ac.hansung.smartrent.domain.recommendation.service.RecommendationService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자 누구나(SecurityConfig). 사용 횟수 제한 없음 */
@Tag(name = "AI 추천")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;

	@Operation(summary = "AI 기자재 추천")
	@PostMapping
	public ApiResponse<RecommendationResponse> recommend(@Valid @RequestBody RecommendRequest request) {
		return ApiResponse.ok(recommendationService.recommend(CurrentUser.id(), request.purpose()));
	}
}
