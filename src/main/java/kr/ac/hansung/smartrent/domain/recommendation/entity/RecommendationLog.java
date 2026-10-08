package kr.ac.hansung.smartrent.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.hansung.smartrent.global.entity.BaseCreatedEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** AI 추천 요청과 결과(비용·품질 확인용). 탈퇴하면 userId만 비워짐 (DB 설계 3절 recommendation_logs) */
@Getter
@Entity
@Table(name = "recommendation_logs")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecommendationLog extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = true)
	private Long userId;

	@Column(name = "purpose", nullable = false, length = 200)
	private String purpose;

	@Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
	private String resultJson;

	@Column(name = "model_id_used", nullable = true, length = 100)
	private String modelIdUsed;
}
