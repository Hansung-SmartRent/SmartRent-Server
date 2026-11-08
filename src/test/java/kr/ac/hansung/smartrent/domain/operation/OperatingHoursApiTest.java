package kr.ac.hansung.smartrent.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.notification.entity.Notification;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationKind;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** B1-20 서버 구현 후 확인할 것 1~3행. 레이저 커팅기(모델 29)는 09:00~20:00, 60분 간격 */
class OperatingHoursApiTest extends RentalSupport {

	@BeforeEach
	void clean() {
		clearRentals();
		em.createQuery("delete from OperatingHours").executeUpdate();
		em.createQuery("delete from Notification").executeUpdate();
	}

	private ResultActions change(Long modelId, String json) throws Exception {
		return mvc.perform(put("/api/v1/admin/models/" + modelId + "/hours").header("Authorization", adminToken())
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions availability(Long modelId, String date) throws Exception {
		return mvc.perform(get("/api/v1/models/" + modelId + "/availability").param("date", date)
			.header("Authorization", studentToken()));
	}

	@Test
	void B1_20_1_기본_시간을_10시_18시로_줄이면_17시_19시_예약만_취소와_알림과_목록() throws Exception {
		EquipmentModel m = model("29");
		Rental late = record(m, other(), RentalStatus.RESERVED, "17:00", "19:00");
		Rental morning = record(m, student(), RentalStatus.RESERVED, "10:00", "11:00");
		em.flush();

		change(m.getId(), "{\"openTime\":\"10:00\",\"closeTime\":\"18:00\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.cancelledRentals.length()").value(1))
			.andExpect(jsonPath("$.data.cancelledRentals[0].rentalId").value(late.getId()))
			.andExpect(jsonPath("$.data.cancelledRentals[0].startAt").value("2026-10-07T17:00:00+09:00"));
		em.flush();
		em.refresh(late);
		em.refresh(morning);

		assertThat(late.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(morning.getStatus()).isEqualTo(RentalStatus.RESERVED);
		assertThat(em.createQuery("select n from Notification n where n.userId = :id", Notification.class)
			.setParameter("id", other()).getResultList()).singleElement()
			.satisfies(n -> assertThat(n.getKind()).isEqualTo(NotificationKind.CANCEL));
		mvc.perform(get("/api/v1/admin/models/" + m.getId() + "/hours").header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].date").doesNotExist())
			.andExpect(jsonPath("$.data[0].openTime").value("10:00"))
			.andExpect(jsonPath("$.data[0].closeTime").value("18:00"));
	}

	@Test
	void B1_20_2_3_특정_날짜만_12시_15시로_바꾸면_그날만_그_칸_되돌리면_기본_칸() throws Exception {
		Long id = model("29").getId();
		availability(id, "2026-10-08").andExpect(status().isOk()).andExpect(jsonPath("$.data.slots.length()").value(11));

		change(id, "{\"date\":\"2026-10-08\",\"openTime\":\"12:00\",\"closeTime\":\"15:00\"}").andExpect(status().isOk());
		em.flush();
		availability(id, "2026-10-08").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.slots.length()").value(3))
			.andExpect(jsonPath("$.data.slots[0].start").value("2026-10-08T12:00:00+09:00"))
			.andExpect(jsonPath("$.data.slots[2].end").value("2026-10-08T15:00:00+09:00"));
		availability(id, "2026-10-09").andExpect(status().isOk());

		mvc.perform(delete("/api/v1/admin/models/" + id + "/hours/2026-10-08").header("Authorization", adminToken()))
			.andExpect(status().isOk());
		em.flush();
		availability(id, "2026-10-08").andExpect(status().isOk()).andExpect(jsonPath("$.data.slots.length()").value(11));
	}

	@Test
	void B1_20_없는_날짜_되돌리기는_404_시작이_끝보다_늦으면_400_학생은_403() throws Exception {
		Long id = model("29").getId();
		mvc.perform(delete("/api/v1/admin/models/" + id + "/hours/2026-10-08").header("Authorization", adminToken()))
			.andExpect(status().isNotFound());
		change(id, "{\"openTime\":\"18:00\",\"closeTime\":\"10:00\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		change(id, "{\"openTime\":\"9시\",\"closeTime\":\"10:00\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(get("/api/v1/admin/models/" + id + "/hours").header("Authorization", studentToken()))
			.andExpect(status().isForbidden());
	}
}
