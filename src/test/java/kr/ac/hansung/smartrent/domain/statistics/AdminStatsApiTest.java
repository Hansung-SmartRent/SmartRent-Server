package kr.ac.hansung.smartrent.domain.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.rental.entity.ApplicationSource;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalApplication;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.entity.ReturnCondition;
import kr.ac.hansung.smartrent.domain.warning.entity.Warning;
import kr.ac.hansung.smartrent.domain.warning.entity.WarningKind;
import kr.ac.hansung.smartrent.global.audit.AuditLogService;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** B1-24 서버 구현 후 확인할 것 1~6행 */
class AdminStatsApiTest extends RentalSupport {

	@Autowired
	AuditLogService audit;

	@BeforeEach
	void clean() {
		clearRentals();
		em.createQuery("delete from Warning").executeUpdate();
		em.createQuery("delete from AuditLog").executeUpdate();
	}

	private static LocalDateTime at(int day, int hour, int minute) {
		return LocalDateTime.of(2026, 10, day, hour, minute);
	}

	private Rental returned(EquipmentModel m, LocalDateTime picked, LocalDateTime returnedAt) {
		var app = RentalApplication.builder().userId(student()).modelId(m.getId()).source(ApplicationSource.RESERVATION).build();
		em.persist(app);
		var rental = Rental.builder().applicationId(app.getId()).userId(student()).modelId(m.getId())
			.unitId(unit(m).getId()).status(RentalStatus.RETURNED).startAt(picked).endAt(picked.plusHours(1))
			.pickedAt(picked).returnedAt(returnedAt).returnCondition(ReturnCondition.NORMAL).build();
		em.persist(rental);
		return rental;
	}

	private Warning warning(WarningKind kind, LocalDateTime createdAt) {
		Warning w = Warning.builder().userId(student()).kind(kind).createdAt(createdAt).build();
		em.persist(w);
		return w;
	}

	private ResultActions stats(String from, String to) throws Exception {
		return mvc.perform(get("/api/v1/admin/stats").param("from", from).param("to", to).header("Authorization", adminToken()));
	}

	@Test
	void B1_24_1_10월_7일_하루면_대여1_반납1_노쇼1_연체1_인기_모델_1() throws Exception {
		EquipmentModel m = model("29");
		returned(m, at(7, 9, 0), at(7, 10, 0));
		returned(m, at(6, 9, 0), at(8, 10, 0));
		warning(WarningKind.NO_SHOW, at(7, 11, 10));
		warning(WarningKind.LATE, at(7, 12, 0));
		em.flush();

		stats("2026-10-07", "2026-10-07").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.period.from").value("2026-10-07"))
			.andExpect(jsonPath("$.data.counts.rentalCount").value(1))
			.andExpect(jsonPath("$.data.counts.returnCount").value(1))
			.andExpect(jsonPath("$.data.counts.noShowCount").value(1))
			.andExpect(jsonPath("$.data.counts.lateCount").value(1))
			.andExpect(jsonPath("$.data.topModels.length()").value(1))
			.andExpect(jsonPath("$.data.topModels[0].modelId").value(m.getId()))
			.andExpect(jsonPath("$.data.topModels[0].rentalCount").value(1))
			// 현재 상태별 수는 기간과 무관한 전체(반납 2건)
			.andExpect(jsonPath("$.data.byStatus.RETURNED").value(2))
			.andExpect(jsonPath("$.data.byStatus.RENTING").value(0));
	}

	@Test
	void B1_24_2_수령_처리_뒤_관리_기록_맨_위에_수령() throws Exception {
		EquipmentModel m = model("48");
		Rental r = record(m, student(), RentalStatus.RESERVED, "10:00", "12:00");
		audit.record("MODEL_EDIT", "이전 기록", admin());
		em.flush();
		at("2026-10-07T09:05:00+09:00");
		mvc.perform(post("/api/v1/admin/rentals/" + r.getId() + "/pickup").header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"unitCode\":\"" + unit(m).getCode() + "\",\"professorMailChecked\":false}"))
			.andExpect(status().isOk());

		mvc.perform(get("/api/v1/admin/audit-logs").header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].action").value("PICKUP"))
			.andExpect(jsonPath("$.data.items[0].actorName").value("대여 관리자"))
			.andExpect(jsonPath("$.data.items[0].createdAt").value("2026-10-07T09:05:00+09:00"));
	}

	@Test
	void B1_24_3_from이_to보다_뒤거나_날짜_형식_오류면_400() throws Exception {
		stats("2026-10-08", "2026-10-07").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		stats("2026-13-01", "2026-10-07").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(get("/api/v1/admin/stats").param("from", "2026-10-07").header("Authorization", adminToken()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(get("/api/v1/admin/audit-logs").param("from", "2026-10-08").param("to", "2026-10-07")
				.header("Authorization", adminToken()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(get("/api/v1/admin/audit-logs").param("from", "2026-10-07").header("Authorization", adminToken()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void B1_24_4_기간에_기록이_없으면_0과_빈_배열() throws Exception {
		stats("2026-01-01", "2026-01-31").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.counts.rentalCount").value(0))
			.andExpect(jsonPath("$.data.counts.returnCount").value(0))
			.andExpect(jsonPath("$.data.counts.noShowCount").value(0))
			.andExpect(jsonPath("$.data.counts.lateCount").value(0))
			.andExpect(jsonPath("$.data.topModels").isEmpty());
	}

	@Test
	void B1_24_5_actions로_두_종류만_기간_밖_제외_필터_뒤_쪽_나누기() throws Exception {
		at("2026-10-07T09:00:00+09:00");
		audit.record("PICKUP", "수령 1", admin());
		audit.record("WALK_IN", "현장 대여 1", admin());
		audit.record("RETURN", "반납 1", admin());
		audit.record("PICKUP", "수령 2", admin());
		at("2026-10-08T09:00:00+09:00");
		audit.record("PICKUP", "기간 밖 수령", admin());
		em.flush();

		mvc.perform(get("/api/v1/admin/audit-logs").param("actions", "PICKUP,WALK_IN").param("from", "2026-10-07")
				.param("to", "2026-10-07").param("size", "2").header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.page.totalElements").value(3))
			.andExpect(jsonPath("$.data.page.totalPages").value(2))
			.andExpect(jsonPath("$.data.items.length()").value(2))
			.andExpect(jsonPath("$.data.items[0].detail").value("수령 2"));
		mvc.perform(get("/api/v1/admin/audit-logs").param("actions", "PICKUP,WALK_IN").param("from", "2026-10-07")
				.param("to", "2026-10-07").param("size", "2").param("page", "1").header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items.length()").value(1))
			.andExpect(jsonPath("$.data.items[0].detail").value("수령 1"));
		mvc.perform(get("/api/v1/admin/audit-logs").param("action", "RETURN").param("actions", "PICKUP")
				.header("Authorization", adminToken()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void B1_24_6_관리자_취소_경고는_빼고_정지_종료로_만료된_경고는_넣어_연체_1() throws Exception {
		Warning revoked = warning(WarningKind.LATE, at(7, 12, 0));
		revoked.revoke(at(7, 13, 0), "잘못 부여");
		Warning expired = warning(WarningKind.LATE, at(7, 14, 0));
		expired.expire(at(7, 15, 0));
		em.flush();

		stats("2026-10-07", "2026-10-07").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.counts.lateCount").value(1));
	}

	@Test
	void B1_24_학생은_403() throws Exception {
		mvc.perform(get("/api/v1/admin/stats").param("from", "2026-10-07").param("to", "2026-10-07")
				.header("Authorization", studentToken()))
			.andExpect(status().isForbidden());
	}
}
