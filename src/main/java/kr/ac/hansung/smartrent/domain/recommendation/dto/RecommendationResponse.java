package kr.ac.hansung.smartrent.domain.recommendation.dto;

import java.util.List;

/** openapi.yaml Recommendation. 대여 가능 여부는 넣지 않음(기획안 10장) */
public record RecommendationResponse(List<Item> items, boolean modelIdVerified) {

	public record Item(Long modelId, String name, String reason) {
	}
}
