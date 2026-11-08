package kr.ac.hansung.smartrent.domain.statistics.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.statistics.dto.StatsResponse;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.warning.entity.WarningKind;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용 통계(B1-24, 기획안 11장, API 명세 3절 통계 집계 C-03 확정).
 * 기간은 한국 시각 from 00:00 이상 to 다음 날 00:00 미만.
 * 대여 수 = picked_at이 기간 안인 기록, 반납 수 = returned_at이 기간 안인 반납 완료 기록,
 * 노쇼·연체 수 = created_at이 기간 안인 NO_SHOW·LATE 경고(관리자 취소 경고 제외, 정지 종료로 만료된 경고는 포함),
 * 많이 빌린 모델 = 같은 기간 수령 기록을 모델별로 세어 내림차순, 동률은 modelId 오름차순, 0건 제외.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

	private final EntityManager em;
	private final Clock clock;

	public StatsResponse stats(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "시작일이 종료일보다 뒤입니다.");
		}
		LocalDateTime start = from.atStartOfDay();
		LocalDateTime end = to.plusDays(1).atStartOfDay();
		LocalDateTime now = LocalDateTime.now(clock);

		Map<String, Long> byStatus = new LinkedHashMap<>();
		for (RentalStatus status : RentalStatus.values()) {
			byStatus.put(status.name(), 0L);
		}
		for (Object[] row : em.createQuery("select r.status, count(r) from Rental r group by r.status", Object[].class)
			.getResultList()) {
			byStatus.put(((RentalStatus) row[0]).name(), (Long) row[1]);
		}

		long rentalCount = count("select count(r) from Rental r where r.pickedAt >= :start and r.pickedAt < :end", start, end);
		long returnCount = em.createQuery("select count(r) from Rental r where r.status = :status"
				+ " and r.returnedAt >= :start and r.returnedAt < :end", Long.class)
			.setParameter("status", RentalStatus.RETURNED).setParameter("start", start).setParameter("end", end)
			.getSingleResult();
		List<StatsResponse.TopModel> topModels = em.createQuery("select r.modelId, m.name, count(r) from Rental r"
				+ " join EquipmentModel m on m.id = r.modelId where r.pickedAt >= :start and r.pickedAt < :end"
				+ " group by r.modelId, m.name order by count(r) desc, r.modelId asc", Object[].class)
			.setParameter("start", start).setParameter("end", end).getResultList().stream()
			.map(row -> new StatsResponse.TopModel((Long) row[0], (String) row[1], (Long) row[2]))
			.toList();

		return new StatsResponse(byStatus,
			em.createQuery("select count(m) from EquipmentModel m where m.archived = false", Long.class).getSingleResult(),
			em.createQuery("select count(u) from EquipmentUnit u join EquipmentModel m on m.id = u.modelId"
					+ " where u.condition = :normal and u.deletedAt is null and m.archived = false", Long.class)
				.setParameter("normal", UnitCondition.NORMAL).getSingleResult(),
			em.createQuery("select count(u) from User u where u.role = :role and u.approval = :pending", Long.class)
				.setParameter("role", Role.STUDENT).setParameter("pending", Approval.PENDING).getSingleResult(),
			em.createQuery("select count(i) from Inquiry i where i.response is null", Long.class).getSingleResult(),
			em.createQuery("select count(p) from PurchaseRequest p where p.response is null", Long.class).getSingleResult(),
			em.createQuery("select count(w) from Warning w where w.revokedAt is null"
					+ " and (w.expiresAt is null or w.expiresAt > :now)", Long.class)
				.setParameter("now", now).getSingleResult(),
			new StatsResponse.Period(from, to),
			new StatsResponse.Counts(rentalCount, returnCount, warnings(WarningKind.NO_SHOW, start, end),
				warnings(WarningKind.LATE, start, end)),
			topModels);
	}

	private long count(String jpql, LocalDateTime start, LocalDateTime end) {
		return em.createQuery(jpql, Long.class).setParameter("start", start).setParameter("end", end).getSingleResult();
	}

	private long warnings(WarningKind kind, LocalDateTime start, LocalDateTime end) {
		return em.createQuery("select count(w) from Warning w where w.kind = :kind and w.revokedAt is null"
				+ " and w.createdAt >= :start and w.createdAt < :end", Long.class)
			.setParameter("kind", kind).setParameter("start", start).setParameter("end", end).getSingleResult();
	}
}
