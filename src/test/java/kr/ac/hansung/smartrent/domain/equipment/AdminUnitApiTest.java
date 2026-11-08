package kr.ac.hansung.smartrent.domain.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/** B1-18 서버 구현 후 확인할 것 1~4행(사례 UNIT-01 둘째 입력, B1-13 4행). 레이저 커팅기(모델 29)는 기기 80·81 두 대 */
class AdminUnitApiTest extends RentalSupport {

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

	private EquipmentUnit unitByCode(String code) {
		return em.createQuery("select u from EquipmentUnit u where u.code = :code", EquipmentUnit.class)
			.setParameter("code", code).getSingleResult();
	}

	private List<Notification> notificationsOf(Long userId) {
		return em.createQuery("select n from Notification n where n.userId = :id", Notification.class)
			.setParameter("id", userId).getResultList();
	}

	@Test
	void B1_18_1_이미_있는_코드로_기기_추가하면_409_UNIT_CODE_DUPLICATE() throws Exception {
		Long modelId = model("29").getId();
		mvc.perform(post("/api/v1/admin/models/" + modelId + "/units").header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"80\",\"name\":\"같은 코드\"}"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("UNIT_CODE_DUPLICATE"));

		mvc.perform(post("/api/v1/admin/models/" + modelId + "/units").header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"b118-new\",\"name\":\"LD-1390 3번\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.code").value("b118-new"))
			.andExpect(jsonPath("$.data.condition").value("NORMAL"));
	}

	@Test
	void B1_18_2_대여_중인_기기_삭제는_409_UNIT_DELETE_BLOCKED() throws Exception {
		Rental renting = record(model("29"), student(), RentalStatus.RENTING, "09:00", "12:00");
		em.flush();

		mvc.perform(delete("/api/v1/admin/units/" + renting.getUnitId()).header("Authorization", adminToken()))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("UNIT_DELETE_BLOCKED"));
		assertThat(em.find(EquipmentUnit.class, renting.getUnitId()).getDeletedAt()).isNull();
	}

	@Test
	void B1_18_3_기기_1대_삭제하면_나중_신청_B만_취소와_알림() throws Exception {
		EquipmentModel m = model("29");
		Rental a = record(m, student(), RentalStatus.RESERVED, "14:00", "15:00");
		Rental b = record(m, taeo(), RentalStatus.RESERVED, "14:00", "15:00");
		em.flush();

		mvc.perform(delete("/api/v1/admin/units/" + unitByCode("81").getId()).header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.cancelledRentals.length()").value(1))
			.andExpect(jsonPath("$.data.cancelledRentals[0].rentalId").value(b.getId()));
		em.flush();
		em.refresh(a);
		em.refresh(b);

		assertThat(unitByCode("81").getDeletedAt()).isNotNull();
		assertThat(a.getStatus()).isEqualTo(RentalStatus.RESERVED);
		assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(notificationsOf(taeo())).singleElement()
			.satisfies(n -> assertThat(n.getKind()).isEqualTo(NotificationKind.CANCEL));
		assertThat(notificationsOf(student())).isEmpty();
	}

	@Test
	void B1_18_4_UNIT_01_관리자가_기기_81을_점검으로_바꾸면_B만_취소_경고_없음() throws Exception {
		EquipmentModel m = model("29");
		Rental r = em.find(Rental.class, record(m, other(), RentalStatus.RENTING, "09:00", "12:00").getId());
		Rental a = record(m, student(), RentalStatus.RESERVED, "14:00", "15:00");
		Rental b = record(m, taeo(), RentalStatus.RESERVED, "14:00", "15:00");
		em.flush();
		EquipmentUnit other = unitByCode("80").getId().equals(r.getUnitId()) ? unitByCode("81") : unitByCode("80");

		mvc.perform(patch("/api/v1/admin/units/" + other.getId()).header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"condition\":\"INSPECTION\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.cancelledRentals.length()").value(1))
			.andExpect(jsonPath("$.data.cancelledRentals[0].rentalId").value(b.getId()));
		em.flush();
		em.refresh(other);
		em.refresh(a);
		em.refresh(b);

		assertThat(other.getCondition()).isEqualTo(UnitCondition.INSPECTION);
		assertThat(a.getStatus()).isEqualTo(RentalStatus.RESERVED);
		assertThat(b.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(notificationsOf(taeo())).singleElement()
			.satisfies(n -> assertThat(n.getKind()).isEqualTo(NotificationKind.CANCEL));
		assertThat(warnings.count(taeo())).isZero();
		assertThat(warnings.count(other())).isZero();
	}

	@Test
	void B1_18_수리가_끝나_NORMAL로_되돌리면_예약_정리_없음_이름도_바뀜() throws Exception {
		EquipmentUnit unit = unitByCode("81");
		unit.changeCondition(UnitCondition.INSPECTION);
		em.flush();

		mvc.perform(patch("/api/v1/admin/units/" + unit.getId()).header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"LD-1390 수리 완료\",\"condition\":\"NORMAL\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.cancelledRentals").isEmpty());
		em.flush();
		em.refresh(unit);
		assertThat(unit.getCondition()).isEqualTo(UnitCondition.NORMAL);
		assertThat(unit.getName()).isEqualTo("LD-1390 수리 완료");
	}

	@Test
	void B1_18_지운_기기는_404_학생은_403() throws Exception {
		EquipmentUnit unit = unitByCode("81");
		mvc.perform(delete("/api/v1/admin/units/" + unit.getId()).header("Authorization", adminToken()))
			.andExpect(status().isOk());
		mvc.perform(delete("/api/v1/admin/units/" + unit.getId()).header("Authorization", adminToken()))
			.andExpect(status().isNotFound());
		mvc.perform(patch("/api/v1/admin/units/" + unitByCode("80").getId()).header("Authorization", studentToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"condition\":\"INSPECTION\"}"))
			.andExpect(status().isForbidden());
	}
}
