package kr.ac.hansung.smartrent.domain.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import kr.ac.hansung.smartrent.domain.statistics.dto.AuditLogResponse;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리 기록 조회(B1-24, API 명세 3절 관리자 조회). 기간은 한국 날짜 양끝 포함, 여러 action은 OR, 기간과 종류는 AND.
 * 날짜 한쪽만 보내거나 시작일이 종료일보다 뒤면, 또는 action과 actions를 함께 보내면 VALIDATION_ERROR.
 * 필터를 DB에서 먼저 적용한 뒤 최신 순으로 쪽을 나눕니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

	private final EntityManager em;

	public PageResponse<AuditLogResponse> list(String action, List<String> actions, LocalDate from, LocalDate to,
		int page, int size) {
		if ((from == null) != (to == null)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "from과 to는 함께 보내야 합니다.");
		}
		if (from != null && from.isAfter(to)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "시작일이 종료일보다 뒤입니다.");
		}
		boolean hasAction = action != null && !action.isBlank();
		List<String> kinds = actions == null ? List.of()
			: actions.stream().filter(a -> a != null && !a.isBlank()).map(String::trim).toList();
		if (hasAction && !kinds.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "action과 actions는 함께 쓸 수 없습니다.");
		}
		if (hasAction) {
			kinds = List.of(action.trim());
		}

		StringBuilder where = new StringBuilder(" where 1 = 1");
		if (from != null) {
			where.append(" and a.createdAt >= :start and a.createdAt < :end");
		}
		if (!kinds.isEmpty()) {
			where.append(" and a.action in :kinds");
		}
		TypedQuery<Long> countQuery = em.createQuery("select count(a) from AuditLog a" + where, Long.class);
		TypedQuery<Object[]> listQuery = em.createQuery("select a.id, a.action, a.detail, u.name, a.createdAt from AuditLog a"
			+ " left join User u on u.id = a.actorId" + where + " order by a.createdAt desc, a.id desc", Object[].class);
		for (TypedQuery<?> query : List.of(countQuery, listQuery)) {
			if (from != null) {
				query.setParameter("start", from.atStartOfDay()).setParameter("end", to.plusDays(1).atStartOfDay());
			}
			if (!kinds.isEmpty()) {
				query.setParameter("kinds", kinds);
			}
		}
		long total = countQuery.getSingleResult();
		List<AuditLogResponse> items = listQuery.setFirstResult((int) Math.min((long) page * size, Integer.MAX_VALUE))
			.setMaxResults(size).getResultList().stream()
			.map(row -> new AuditLogResponse((Long) row[0], (String) row[1], (String) row[2], (String) row[3],
				(LocalDateTime) row[4]))
			.toList();
		long totalPages = total == 0 ? 0 : (total + size - 1) / size;
		return new PageResponse<>(items, new PageResponse.Meta(page, size, total, totalPages));
	}
}
