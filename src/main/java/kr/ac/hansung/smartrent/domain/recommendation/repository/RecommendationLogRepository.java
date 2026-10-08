package kr.ac.hansung.smartrent.domain.recommendation.repository;

import kr.ac.hansung.smartrent.domain.recommendation.entity.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {
}
