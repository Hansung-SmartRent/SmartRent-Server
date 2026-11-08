package kr.ac.hansung.smartrent.domain.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.notification.entity.Notification;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationKind;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** B1-13 서버 구현 후 확인할 것 1~8행(사례 UNIT-01, RET-02). 레이저 커팅기(모델 29)는 기기 80·81 두 대 */
class DamageReportApiTest extends RentalSupport {

	@Autowired
	WarningService warnings;

	@BeforeEach
	void clean() {
		clearRentals();
		em.createQuery("delete from Warning").executeUpdate();
		em.createQuery("delete from Notification").executeUpdate();
		at("2026-10-07T10:30:00+09:00");
	}

	private Long taeo() {
		return users.findByEmail("taeo@hansung.ac.kr").orElseThrow().getId();
	}

	private ResultActions report(Rental rental, String token) throws Exception {
		return mvc.perform(post("/api/v1/rentals/" + rental.getId() + "/damage-report").header("Authorization", token)
			.contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"렌즈 앞쪽에 금이 가 있습니다.\"}"));
	}

	private EquipmentUnit unitOf(Rental rental) {
		return em.find(EquipmentUnit.class, rental.getUnitId());
	}

	private List<Notification> notificationsOf(Long userId) {
		return em.createQuery("select n from Notification n where n.userId = :id", Notification.class)
			.setParameter("id", userId).getResultList();
	}

	@Test
	void B1_13_1_본인_대여에_신고하면_표시_기기_점검_대여_그대로_관리자_알림() throws Exception {
		Rental rental = record(model("29"), student(), RentalStatus.RENTING, "09:00", "12:00");
		em.flush();

		report(rental, studentToken()).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(rental.getId()))
			.andExpect(jsonPath("$.data.status").value("RENTING"))
			.andExpect(jsonPath("$.data.brokenReported").value(true))
			.andExpect(jsonPath("$.data.endAt").value("2026-10-07T12:00:00+09:00"));
		em.flush();
		em.refresh(rental);

		assertThat(rental.isBrokenReported()).isTrue();
		assertThat(rental.getBrokenNote()).isEqualTo("렌즈 앞쪽에 금이 가 있습니다.");
		assertThat(rental.getEndAt()).isEqualTo(time("12:00"));
		assertThat(unitOf(rental).getCondition()).isEqualTo(UnitCondition.INSPECTION);
		assertThat(notificationsOf(admin())).singleElement().satisfies(n -> {
			assertThat(n.getKind()).isEqualTo(NotificationKind.DAMAGE);
			assertThat(n.getRelatedId()).isEqualTo(rental.getId());
		});
	}

	@Test
	void B1_13_2_남은_정상_기기로_줄_수_있으면_예약_그대로() throws Exception {
		EquipmentModel m = model("29");
		Rental rental = record(m, other(), RentalStatus.RENTING, "09:00", "12:00");
		Rental later = record(m, student(), RentalStatus.RESERVED, "14:00", "15:00");
		em.flush();

		report(rental, token(other(), "STUDENT")).andExpect(status().isOk());
		em.flush();
		em.refresh(later);
		assertThat(later.getStatus()).isEqualTo(RentalStatus.RESERVED);
	}

	@Test
	void B1_13_3_UNIT_01_학생_신고면_나중_신청_B만_취소_경고_없음() throws Exception {
		EquipmentModel m = model("29");
		Rental r = record(m, other(), RentalStatus.RENTING, "09:00", "12:00");
		Rental a = record(m, student(), RentalStatus.RESERVED, "14:00", "15:00");
		Rental b = record(m, taeo(), RentalStatus.RESERVED, "14:00", "15:00");
		em.flush();

		report(r, token(other(), "STUDENT")).andExpect(status().isOk());
		em.flush();
		em.refresh(r);
		em.refresh(a);
		em.refresh(b);

		assertThat(unitOf(r).getCondition()).isEqualTo(UnitCondition.INSPECTION);
		assertThat(r.getStatus()).isEqualTo(RentalStatus.RENTING);
		assertThat(r.getEndAt()).isEqualTo(time("12:00"));
		assertThat(a.getStatus()).isEqualTo(RentalStatus.RESERVED);
		assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(notificationsOf(taeo())).singleElement()
			.satisfies(n -> assertThat(n.getKind()).isEqualTo(NotificationKind.CANCEL));
		assertThat(warnings.count(other())).isZero();
		assertThat(warnings.count(taeo())).isZero();
	}

	@Test
	void B1_13_5_남의_대여에_신고하면_403() throws Exception {
		Rental rental = record(model("29"), student(), RentalStatus.RENTING, "09:00", "12:00");
		em.flush();

		report(rental, token(other(), "STUDENT")).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		em.refresh(rental);
		assertThat(rental.isBrokenReported()).isFalse();
	}

	@Test
	void B1_13_6_반납된_대여에_신고하면_409_INVALID_STATUS() throws Exception {
		Rental rental = record(model("29"), student(), RentalStatus.RETURNED, "08:00", "09:00");
		em.flush();

		report(rental, studentToken()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("INVALID_STATUS"));
	}

	@Test
	void B1_13_7_RET_02_신고한_대여를_파손으로_반납하면_경고_0() throws Exception {
		Rental rental = record(model("29"), student(), RentalStatus.RENTING, "09:00", "12:00");
		em.flush();
		report(rental, studentToken()).andExpect(status().isOk());
		em.flush();

		at("2026-10-07T11:00:00+09:00");
		mvc.perform(post("/api/v1/admin/rentals/" + rental.getId() + "/return").header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"unitCode\":\"" + unitOf(rental).getCode() + "\",\"condition\":\"DAMAGED\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RETURNED"));
		assertThat(warnings.count(student())).isZero();
	}

	@Test
	void B1_13_8_신고자_응답에는_본인_대여만_취소된_예약자에게는_본인_알림() throws Exception {
		EquipmentModel m = model("29");
		Rental r = record(m, other(), RentalStatus.RENTING, "09:00", "12:00");
		record(m, student(), RentalStatus.RESERVED, "14:00", "15:00");
		Rental b = record(m, taeo(), RentalStatus.RESERVED, "14:00", "15:00");
		em.flush();

		String body = report(r, token(other(), "STUDENT")).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain("윤태오").doesNotContain("cancelled").doesNotContain("\"id\":" + b.getId() + ",");
		assertThat(notificationsOf(taeo())).singleElement()
			.satisfies(n -> assertThat(n.getRelatedId()).isEqualTo(b.getId()));
	}

	@Test
	void B1_13_내용이_비면_400_관리자는_403() throws Exception {
		Rental rental = record(model("29"), student(), RentalStatus.RENTING, "09:00", "12:00");
		em.flush();
		mvc.perform(post("/api/v1/rentals/" + rental.getId() + "/damage-report").header("Authorization", studentToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"note\":\" \"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		report(rental, adminToken()).andExpect(status().isForbidden());
	}
}
