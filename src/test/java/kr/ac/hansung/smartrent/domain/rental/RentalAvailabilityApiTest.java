package kr.ac.hansung.smartrent.domain.rental;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.operation.entity.OperatingHours;
import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, RentalAvailabilityApiTest.Time.class})
@Transactional
class RentalAvailabilityApiTest {
	static final Instant NOW = OffsetDateTime.parse("2026-10-07T08:00:00+09:00").toInstant();
	@TestConfiguration static class Time {
		@Bean @Primary MutableClock availabilityClock() { return new MutableClock(NOW); }
	}
	@Autowired MockMvc mvc;
	@Autowired MutableClock clock;
	@Autowired EntityManager em;
	@Autowired UserRepository users;
	@Autowired JwtEncoder encoder;
	long id;
	@BeforeEach void prepare() {
		clock.set(NOW);
		id = em.createQuery("select m from EquipmentModel m where m.modelKey = '17'", EquipmentModel.class)
			.getSingleResult().getId();
	}
	String url() { return "/api/v1/models/" + id + "/availability"; }
	String token(String email, String role) {
		return TestTokens.bearer(encoder, users.findByEmail(email).orElseThrow().getId(), role, NOW.plusSeconds(1800));
	}
	@Test void B2_02_API_permissions_and_response() throws Exception {
		for (var user : users.findAll()) {
			mvc.perform(get(url()).param("date", "2026-10-07")
				.header("Authorization", token(user.getEmail(), user.getRole().name())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.errorCode").isEmpty()).andExpect(jsonPath("$.data.closed").value(false))
				.andExpect(jsonPath("$.data.slots.length()").value(22))
				.andExpect(jsonPath("$.data.slots[0].start").value("2026-10-07T09:30:00+09:00"));
		}
	}
	@Test void B2_02_API_errors() throws Exception {
		mvc.perform(get(url()).param("date", "2026-10-07")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
		var token = token("demo@hansung.ac.kr", "STUDENT");
		for (var value : new String[]{null, "invalid", "2026-02-30"}) {
			var request = get(url()).header("Authorization", token);
			if (value != null) request.param("date", value);
			mvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		}
		mvc.perform(get("/api/v1/models/9223372036854775807/availability").param("date", "2026-10-07")
			.header("Authorization", token)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}
	@Test void B2_02_02_RSV_05_calendar_from_database() throws Exception {
		mvc.perform(get(url()).param("date", "2026-10-09").header("Authorization", token("demo@hansung.ac.kr", "STUDENT")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.closed").value(true))
			.andExpect(jsonPath("$.data.closedReason").value("한글날"))
			.andExpect(jsonPath("$.data.slots").isEmpty());
	}
	@Test void B2_02_API_hours_specific_default_model_priority() throws Exception {
		var defaults = OperatingHours.builder().modelId(id).openTime(LocalTime.of(10, 0)).closeTime(LocalTime.of(18, 0)).build();
		var specific = OperatingHours.builder().modelId(id).date(LocalDate.of(2026, 10, 7))
			.openTime(LocalTime.NOON).closeTime(LocalTime.of(15, 10)).build();
		em.persist(defaults);
		em.persist(specific);
		var token = token("demo@hansung.ac.kr", "STUDENT");
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots.length()").value(6))
			.andExpect(jsonPath("$.data.slots[0].start").value("2026-10-07T12:00:00+09:00"));
		em.remove(specific);
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots.length()").value(16));
		em.remove(defaults);
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots.length()").value(22));
	}
	@Test void B2_02_03_04_05_seeded_models() throws Exception {
		var token = token("demo@hansung.ac.kr", "STUDENT");
		id = modelId("30");
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots").isEmpty())
			.andExpect(jsonPath("$.data.longTerm.pickupDate").value("2026-10-07"))
			.andExpect(jsonPath("$.data.longTerm.returnDue").value("2027-04-07T17:00:00+09:00"));
		id = modelId("demo-lg");
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.multiDayReturnDates.length()").value(6))
			.andExpect(jsonPath("$.data.multiDayReturnDates[5]").value("2026-10-16"));
		id = modelId("15");
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.multiDayReturnDates.length()").value(4))
			.andExpect(jsonPath("$.data.multiDayReturnDates[0]").value("2026-10-08"))
			.andExpect(jsonPath("$.data.multiDayReturnDates[1]").value("2026-10-12"))
			.andExpect(jsonPath("$.data.multiDayReturnDates[2]").value("2026-10-13"))
			.andExpect(jsonPath("$.data.multiDayReturnDates[3]").value("2026-10-14"));
	}
	long modelId(String key) {
		return em.createQuery("select m from EquipmentModel m where m.modelKey = :key", EquipmentModel.class)
			.setParameter("key", key).getSingleResult().getId();
	}
	@Test void B2_02_API_capacity_and_carry_out_buffer() throws Exception {
		id = modelId("demo-lg");
		long userId = users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId();
		var application = RentalApplication.builder().userId(userId).modelId(id).source(ApplicationSource.RESERVATION).build();
		em.persist(application);
		for (int i = 0; i < 2; i++) {
			em.persist(Rental.builder().applicationId(application.getId()).userId(userId).modelId(id)
				.status(RentalStatus.RESERVED).startAt(LocalDateTime.of(2026, 10, 7, 14, 0))
				.endAt(LocalDateTime.of(2026, 10, 7, 16, 0)).build());
		}
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token("demo@hansung.ac.kr", "STUDENT")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.slots[3].available").value(2))
			.andExpect(jsonPath("$.data.slots[4].available").value(0));
	}
	@Test void B2_02_API_holiday_override_and_archived() throws Exception {
		var holiday = em.find(Holiday.class, LocalDate.of(2026, 10, 9));
		em.remove(holiday);
		em.flush();
		em.persist(Holiday.builder().date(holiday.getDate()).name(holiday.getName()).source(holiday.getSource())
			.openOverride(true).build());
		var token = token("demo@hansung.ac.kr", "STUDENT");
		mvc.perform(get(url()).param("date", "2026-10-09").header("Authorization", token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.closed").value(false))
			.andExpect(jsonPath("$.data.closedReason").isEmpty()).andExpect(jsonPath("$.data.slots.length()").value(22));
		em.createQuery("update EquipmentModel m set m.archived = true where m.id = :id").setParameter("id", id).executeUpdate();
		em.clear();
		mvc.perform(get(url()).param("date", "2026-10-07").header("Authorization", token))
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}
}
