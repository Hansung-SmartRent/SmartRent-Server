package kr.ac.hansung.smartrent.domain.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

/** B1-17 서버 구현 후 확인할 것 1~8행. 시각은 2026-10-07(수) 09:00 */
class AdminModelApiTest extends RentalSupport {

	@BeforeEach
	void clean() {
		clearRentals();
		em.createQuery("delete from Notification").executeUpdate();
	}

	private static String body(String name, boolean multiDay, Integer maxDays, Integer maxMinutes, String open, String close) {
		return "{\"category\":\"촬영\",\"name\":\"" + name + "\",\"manufacturer\":\"제조사\",\"purchaseYear\":\"2024\","
			+ "\"purpose\":\"시험\",\"rentalType\":\"GENERAL\",\"multiDay\":" + multiDay + ",\"maxRentalDays\":" + maxDays
			+ ",\"maxRentalMinutes\":" + maxMinutes + ",\"carryOut\":false,\"location\":\"공학관 101호\","
			+ "\"contact\":null,\"openTime\":\"" + open + "\",\"closeTime\":\"" + close + "\",\"intervalMinutes\":60,\"guide\":null}";
	}

	private static String body(String name) {
		return body(name, false, null, null, "09:00", "20:00");
	}

	/** 이미 있는 모델의 지금 값을 그대로 담고 일부만 바꾼 요청 */
	private static String bodyOf(EquipmentModel m, boolean multiDay, Integer maxMinutes) {
		return "{\"category\":\"" + m.getCategory() + "\",\"name\":\"" + m.getName() + "\",\"rentalType\":\""
			+ m.getRentalType() + "\",\"multiDay\":" + multiDay + ",\"maxRentalDays\":" + m.getMaxRentalDays()
			+ ",\"maxRentalMinutes\":" + maxMinutes + ",\"carryOut\":" + m.isCarryOut() + ",\"location\":\""
			+ m.getLocation() + "\",\"openTime\":\"" + m.getOpenTime().toString().substring(0, 5) + "\",\"closeTime\":\""
			+ m.getCloseTime().toString().substring(0, 5) + "\",\"intervalMinutes\":" + m.getIntervalMinutes() + "}";
	}

	private ResultActions create(String json) throws Exception {
		return mvc.perform(post("/api/v1/admin/models").header("Authorization", adminToken())
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions update(Long id, String json) throws Exception {
		return mvc.perform(put("/api/v1/admin/models/" + id).header("Authorization", adminToken())
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions archive(Long id) throws Exception {
		return mvc.perform(post("/api/v1/admin/models/" + id + "/archive").header("Authorization", adminToken()));
	}

	private List<String> auditDetails(String action) {
		return em.createQuery("select a.detail from AuditLog a where a.action = :action order by a.id desc", String.class)
			.setParameter("action", action).getResultList();
	}

	@Test
	void B1_17_1_모델_추가하면_201_source_ADMIN() throws Exception {
		create(body("새 카메라 X1")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.source").value("ADMIN"))
			.andExpect(jsonPath("$.data.name").value("새 카메라 X1"))
			.andExpect(jsonPath("$.data.archived").value(false))
			.andExpect(jsonPath("$.data.units").isEmpty())
			.andExpect(jsonPath("$.data.openTime").value("09:00"));
		assertThat(auditDetails("MODEL_ADD")).isNotEmpty();
	}

	@Test
	void B1_17_2_대소문자_공백만_다른_이름은_409_MODEL_NAME_DUPLICATE() throws Exception {
		create(body("Test Cam QX")).andExpect(status().isCreated());
		create(body("test cam qx")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("MODEL_NAME_DUPLICATE"));
		create(body("TESTCAM  QX")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("MODEL_NAME_DUPLICATE"));
		// 처음 데이터의 "Insta360 EVO"(완료 조건의 예)
		create(body("insta360 evo")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("MODEL_NAME_DUPLICATE"));
	}

	@Test
	void B1_17_3_운영_종료한_모델과_같은_이름은_201() throws Exception {
		String json = create(body("옛 장비 Z")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Long id = Long.valueOf(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
		archive(id).andExpect(status().isOk());
		create(body("옛 장비 Z")).andExpect(status().isCreated());
	}

	@Test
	void B1_17_4_앞으로의_예약_2건_있는_모델_운영_종료하면_취소와_알림과_목록() throws Exception {
		EquipmentModel m = model("29");
		Rental a = record(m, student(), RentalStatus.RESERVED, "14:00", "15:00");
		Rental b = record(m, other(), RentalStatus.RESERVED, "16:00", "17:00");
		em.flush();

		archive(m.getId()).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.cancelledRentals.length()").value(2))
			.andExpect(jsonPath("$.data.cancelledRentals[0].studentName").isNotEmpty())
			.andExpect(jsonPath("$.data.cancelledRentals[0].modelName").value("레이저 커팅기"))
			.andExpect(jsonPath("$.data.extendedRentals").isEmpty());
		em.flush();
		em.refresh(a);
		em.refresh(b);
		em.refresh(m);

		assertThat(a.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(m.isArchived()).isTrue();
		assertThat(m.getNameKey()).isNull();
		for (Long user : List.of(student(), other())) {
			assertThat(em.createQuery("select n from Notification n where n.userId = :id", Notification.class)
				.setParameter("id", user).getResultList()).singleElement()
				.satisfies(n -> assertThat(n.getKind()).isEqualTo(NotificationKind.CANCEL));
		}
		assertThat(auditDetails("MODEL_ARCHIVE")).isNotEmpty();
	}

	@Test
	void B1_17_5_대여_중인_모델_운영_종료는_409_MODEL_ARCHIVE_BLOCKED() throws Exception {
		EquipmentModel m = model("29");
		record(m, student(), RentalStatus.RENTING, "09:00", "11:00");
		em.flush();

		archive(m.getId()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("MODEL_ARCHIVE_BLOCKED"));
		em.refresh(m);
		assertThat(m.isArchived()).isFalse();
	}

	@Test
	void B1_17_6_일반_모델을_multiDay로_바꾸면_반납일_선택지() throws Exception {
		EquipmentModel m = model("29");
		mvc.perform(get("/api/v1/models/" + m.getId() + "/availability").param("date", "2026-10-07")
				.header("Authorization", studentToken()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.multiDayReturnDates").isEmpty());

		update(m.getId(), bodyOf(m, true, m.getMaxRentalMinutes())).andExpect(status().isOk());
		em.flush();

		mvc.perform(get("/api/v1/models/" + m.getId() + "/availability").param("date", "2026-10-07")
				.header("Authorization", studentToken()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.multiDayReturnDates").isNotEmpty());
	}

	@Test
	void B1_17_7_maxRentalMinutes_120으로_바꾸면_관리_기록에_전후_값_이후_예약에_2시간_제한() throws Exception {
		EquipmentModel m = model("17");
		Integer before = m.getMaxRentalMinutes();
		update(m.getId(), bodyOf(m, m.isMultiDay(), 120)).andExpect(status().isOk());
		em.flush();
		em.refresh(m);

		assertThat(m.getMaxRentalMinutes()).isEqualTo(120);
		assertThat(auditDetails("MODEL_EDIT").get(0)).contains("maxRentalMinutes " + before + " -> 120");
		mvc.perform(post("/api/v1/reservations").header("Authorization", studentToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"modelId\":" + m.getId() + ",\"segments\":[{\"start\":\"2026-10-07T10:30:00+09:00\","
					+ "\"end\":\"2026-10-07T13:30:00+09:00\"}]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("RENTAL_TOO_LONG"));
	}

	@Test
	void B1_17_8_maxRentalDays가_0이나_음수면_400() throws Exception {
		create(body("일수 시험 A", true, 0, null, "09:00", "20:00")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		create(body("일수 시험 B", true, -1, null, "09:00", "20:00")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void B1_17_기본_운영_시간을_줄이면_밖으로_나간_예약만_취소() throws Exception {
		EquipmentModel m = model("29");
		Rental inside = record(m, student(), RentalStatus.RESERVED, "10:00", "11:00");
		Rental outside = record(m, other(), RentalStatus.RESERVED, "17:00", "19:00");
		em.flush();

		update(m.getId(), body("레이저 커팅기", false, null, m.getMaxRentalMinutes(), "10:00", "18:00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.cancelledRentals.length()").value(1))
			.andExpect(jsonPath("$.data.cancelledRentals[0].rentalId").value(outside.getId()));
		em.flush();
		em.refresh(inside);
		assertThat(inside.getStatus()).isEqualTo(RentalStatus.RESERVED);
	}

	@Test
	void B1_17_관리자_상세는_기기와_대여_중_여부_운영_종료_모델도_보임_학생은_403() throws Exception {
		EquipmentModel m = model("29");
		Rental renting = record(m, student(), RentalStatus.RENTING, "09:00", "11:00");
		em.flush();

		mvc.perform(get("/api/v1/admin/models/" + m.getId()).header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.units.length()").value(2))
			.andExpect(jsonPath("$.data.units[?(@.id == " + renting.getUnitId() + ")].inUse").value(true))
			.andExpect(jsonPath("$.data.todayHours.open").value("09:00"));
		mvc.perform(get("/api/v1/admin/models/" + m.getId()).header("Authorization", studentToken()))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/admin/models/999999").header("Authorization", adminToken()))
			.andExpect(status().isNotFound());
	}

	@Test
	void B1_17_운영_시작이_끝보다_늦거나_간격이_45분이면_400() throws Exception {
		create(body("시각 시험", false, null, null, "18:00", "09:00")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		create(body("간격 시험").replace("\"intervalMinutes\":60", "\"intervalMinutes\":45"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
