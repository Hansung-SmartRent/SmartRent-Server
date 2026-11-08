package kr.ac.hansung.smartrent.domain.statistics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * openapi Stats. byStatus·models·normalUnits·pendingStudents·unanswered*·activeWarnings는 조회 시점 전체 현황,
 * period·counts·topModels는 기간 집계(API 명세 3절 통계 집계, C-03 확정)
 */
public record StatsResponse(Map<String, Long> byStatus, long models, long normalUnits, long pendingStudents,
	long unansweredInquiries, long unansweredPurchaseRequests, long activeWarnings, Period period, Counts counts,
	List<TopModel> topModels) {

	public record Period(LocalDate from, LocalDate to) {
	}

	public record Counts(long rentalCount, long returnCount, long noShowCount, long lateCount) {
	}

	public record TopModel(Long modelId, String modelName, long rentalCount) {
	}
}
