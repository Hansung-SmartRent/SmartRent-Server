package kr.ac.hansung.smartrent.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.notification.entity.Notification;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationKind;
import kr.ac.hansung.smartrent.domain.operation.client.HolidayClient;
import kr.ac.hansung.smartrent.domain.operation.client.HolidayClient.PublicHoliday;
import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import kr.ac.hansung.smartrent.domain.operation.entity.HolidaySource;
import kr.ac.hansung.smartrent.domain.operation.repository.HolidayRepository;
import kr.ac.hansung.smartrent.domain.operation.service.HolidaySyncService;
import kr.ac.hansung.smartrent.domain.rental.entity.ApplicationSource;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalApplication;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.service.OperationChangeService;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.global.port.OperationsAlert;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

/** B1-19 서버 구현 후 확인할 것 1~6행(사례 OPS-01, OPS-02). 시각은 2026-10-07(수) 09:00, 10/9 한글날은 처음 데이터 */
class HolidayApiTest extends RentalSupport {

	@Autowired
	HolidayRepository holidays;
	@Autowired
	WarningService warnings;
	@Autowired
	OperationChangeService operationChangeService;
	@Autowired
	OperationsAlert operationsAlert;
	@Autowired
	PlatformTransactionManager transactionManager;

	@BeforeEach
	void clean() {
		clearRentals();
		em.createQuery("delete from Warning").executeUpdate();
		em.createQuery("delete from Notification").executeUpdate();
	}

	private Rental rental(EquipmentModel m, Long user, RentalStatus status, String start, String end) {
		var app = RentalApplication.builder().userId(user).modelId(m.getId()).source(ApplicationSource.RESERVATION).build();
		em.persist(app);
		var rental = Rental.builder().applicationId(app.getId()).userId(user).modelId(m.getId()).status(status)
			.startAt(LocalDateTime.parse(start)).endAt(LocalDateTime.parse(end))
			.pickedAt(status == RentalStatus.RENTING ? LocalDateTime.parse(start) : null)
			.unitId(status == RentalStatus.RENTING ? unit(m).getId() : null).build();
		em.persist(rental);
		return rental;
	}

	private ResultActions close(String date, String name) throws Exception {
		return mvc.perform(post("/api/v1/admin/holidays").header("Authorization", adminToken())
			.contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"" + date + "\",\"name\":\"" + name + "\"}"));
	}

	private List<Notification> notificationsOf(Long userId) {
		return em.createQuery("select n from Notification n where n.userId = :id", Notification.class)
			.setParameter("id", userId).getResultList();
	}

	private HolidaySyncService sync(HolidayClient client) {
		return new HolidaySyncService(client, holidays, operationChangeService, operationsAlert, transactionManager, clock);
	}

	@Test
	void B1_19_1_OPS_01_임시_휴무를_넣으면_on8만_취소_on7_그대로_경고_0() throws Exception {
		EquipmentModel m = model("29");
		Rental on8 = rental(m, other(), RentalStatus.RESERVED, "2026-10-08T10:00:00", "2026-10-08T11:00:00");
		Rental on7 = rental(m, student(), RentalStatus.RESERVED, "2026-10-07T15:00:00", "2026-10-07T16:00:00");
		em.flush();

		close("2026-10-08", "기자재실 정기 점검").andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.cancelledRentals.length()").value(1))
			.andExpect(jsonPath("$.data.cancelledRentals[0].rentalId").value(on8.getId()));
		em.flush();
		em.refresh(on8);
		em.refresh(on7);

		assertThat(on8.getStatus()).isEqualTo(RentalStatus.CANCELLED);
		assertThat(on7.getStatus()).isEqualTo(RentalStatus.RESERVED);
		assertThat(warnings.count(other())).isZero();
		assertThat(holidays.findById(LocalDate.of(2026, 10, 8))).get()
			.satisfies(h -> assertThat(h.getSource()).isEqualTo(HolidaySource.ADMIN));
	}

	@Test
	void B1_19_2_OPS_02_반납일이_휴무가_되면_10월_12일_20시로_늘리고_알림() throws Exception {
		Rental due8 = rental(model("29"), student(), RentalStatus.RENTING, "2026-10-07T09:00:00", "2026-10-08T20:00:00");
		em.flush();

		close("2026-10-08", "기자재실 정기 점검").andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.extendedRentals.length()").value(1))
			.andExpect(jsonPath("$.data.extendedRentals[0].newEndAt").value("2026-10-12T20:00:00+09:00"));
		em.flush();
		em.refresh(due8);

		assertThat(due8.getEndAt()).isEqualTo(LocalDateTime.of(2026, 10, 12, 20, 0));
		assertThat(notificationsOf(student())).singleElement().satisfies(n -> {
			assertThat(n.getKind()).isEqualTo(NotificationKind.BOOKING);
			assertThat(n.getTitle()).isEqualTo("반납 기한 변경");
		});
	}

	@Test
	void B1_19_3_관리자가_넣은_임시_휴무_DELETE() throws Exception {
		close("2026-10-14", "기자재실 정기 점검").andExpect(status().isCreated());
		em.flush();

		mvc.perform(delete("/api/v1/admin/holidays/2026-10-14").header("Authorization", adminToken()))
			.andExpect(status().isOk());
		em.flush();
		assertThat(holidays.findById(LocalDate.of(2026, 10, 14))).isEmpty();
		mvc.perform(delete("/api/v1/admin/holidays/2026-10-14").header("Authorization", adminToken()))
			.andExpect(status().isNotFound());
	}

	@Test
	void B1_19_4_한글날을_운영하기로_바꾸면_그날_칸이_나옴_다시_닫을_수_있음() throws Exception {
		Long id = model("29").getId();
		mvc.perform(get("/api/v1/models/" + id + "/availability").param("date", "2026-10-09")
				.header("Authorization", studentToken()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots").isEmpty());

		mvc.perform(post("/api/v1/admin/holidays/2026-10-09/open").header("Authorization", adminToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.source").value("PUBLIC"))
			.andExpect(jsonPath("$.data.openOverride").value(true));
		em.flush();
		mvc.perform(get("/api/v1/models/" + id + "/availability").param("date", "2026-10-09")
				.header("Authorization", studentToken()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots.length()").value(11));

		// 공휴일 운영 해제 취소
		close("2026-10-09", "한글날").andExpect(status().isCreated());
		em.flush();
		em.clear();
		assertThat(holidays.findById(LocalDate.of(2026, 10, 9)).orElseThrow().isOpenOverride()).isFalse();
	}

	@Test
	void B1_19_5_공휴일_받기_2027년은_PUBLIC으로_저장_관리자가_넣은_행은_그대로() {
		holidays.deleteById(LocalDate.of(2027, 3, 1));
		holidays.deleteById(LocalDate.of(2027, 5, 5));
		em.flush();
		holidays.save(Holiday.builder().date(LocalDate.of(2027, 5, 5)).name("관리자 임시 휴무").source(HolidaySource.ADMIN)
			.openOverride(false).build());
		em.flush();
		HolidayClient fake = year -> year == 2027
			? List.of(new PublicHoliday(LocalDate.of(2027, 3, 1), "삼일절"),
				new PublicHoliday(LocalDate.of(2027, 5, 5), "어린이날"))
			: List.of();

		assertThat(sync(fake).sync()).isEqualTo(1);
		em.flush();
		em.clear();

		assertThat(holidays.findById(LocalDate.of(2027, 3, 1))).get().satisfies(h -> {
			assertThat(h.getSource()).isEqualTo(HolidaySource.PUBLIC);
			assertThat(h.getName()).isEqualTo("삼일절");
		});
		assertThat(holidays.findById(LocalDate.of(2027, 5, 5))).get().satisfies(h -> {
			assertThat(h.getSource()).isEqualTo(HolidaySource.ADMIN);
			assertThat(h.getName()).isEqualTo("관리자 임시 휴무");
		});
	}

	@Test
	void B1_19_6_공휴일_받기_오류면_기존_행_그대로_관리자에게_OPERATIONS_1건() {
		long before = holidays.count();
		HolidayClient broken = year -> {
			throw new IllegalStateException("공휴일 응답 오류 코드: 30");
		};

		assertThat(sync(broken).sync()).isEqualTo(-1);
		em.flush();

		assertThat(holidays.count()).isEqualTo(before);
		assertThat(notificationsOf(admin())).singleElement()
			.satisfies(n -> assertThat(n.getKind()).isEqualTo(NotificationKind.OPERATIONS));
	}

	@Test
	void B1_19_휴무일_목록은_학생도_봄_공휴일_운영은_공휴일만_학생은_관리자_주소_403() throws Exception {
		mvc.perform(get("/api/v1/holidays").param("from", "2026-10-01").param("to", "2026-10-31")
				.header("Authorization", studentToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[?(@.date == '2026-10-09')].name").value("한글날"));
		mvc.perform(get("/api/v1/holidays").param("from", "2026-10-31").param("to", "2026-10-01")
				.header("Authorization", studentToken()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(post("/api/v1/admin/holidays/2026-10-14/open").header("Authorization", adminToken()))
			.andExpect(status().isNotFound());
		close("2026-10-14", "점검").andExpect(status().isCreated());
		mvc.perform(post("/api/v1/admin/holidays").header("Authorization", studentToken())
				.contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-10-15\",\"name\":\"점검\"}"))
			.andExpect(status().isForbidden());
	}
}
