package kr.ac.hansung.smartrent.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.auth.entity.EmailVerification;
import kr.ac.hansung.smartrent.domain.auth.entity.VerificationPurpose;
import kr.ac.hansung.smartrent.domain.auth.service.TokenService;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.rental.entity.ApplicationSource;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalApplication;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.warning.entity.Warning;
import kr.ac.hansung.smartrent.domain.warning.entity.WarningKind;
import kr.ac.hansung.smartrent.domain.warning.repository.RetainedWarningRepository;
import kr.ac.hansung.smartrent.support.RentalSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** B1-09 서버 구현 후 확인할 것 1~3행(사례 AUTH-03). 시각은 사례의 2026-10-07T10:00 */
class WithdrawApiTest extends RentalSupport {

	@Autowired
	RetainedWarningRepository retained;
	@Autowired
	TokenService tokenService;
	@Autowired
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void clean() {
		clearRentals();
		em.createQuery("delete from Warning").executeUpdate();
		at("2026-10-07T10:00:00+09:00");
	}

	private EquipmentModel anyModel() {
		return em.createQuery("select m from EquipmentModel m where m.archived = false order by m.id", EquipmentModel.class)
			.setMaxResults(1).getSingleResult();
	}

	private Rental reservation(Long userId, LocalDateTime start, LocalDateTime end) {
		EquipmentModel model = anyModel();
		var app = RentalApplication.builder().userId(userId).modelId(model.getId()).source(ApplicationSource.RESERVATION).build();
		em.persist(app);
		var rental = Rental.builder().applicationId(app.getId()).userId(userId).modelId(model.getId())
			.status(RentalStatus.RESERVED).startAt(start).endAt(end).build();
		em.persist(rental);
		return rental;
	}

	@Test
	void B1_09_1_AUTH_03_대여_중이면_WITHDRAW_BLOCKED_HELD() throws Exception {
		record(anyModel(), student(), RentalStatus.RENTING, "09:00", "11:00");
		em.flush();

		mvc.perform(delete("/api/v1/users/me").header("Authorization", studentToken()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("WITHDRAW_BLOCKED_HELD"));
		assertThat(users.findById(student())).isPresent();
	}

	@Test
	void B1_09_1_AUTH_03_예약은_취소_후_기록_삭제_경고는_학번과_6개월_보관() throws Exception {
		Long id = student();
		String token = studentToken();
		Rental reserved = reservation(id, LocalDateTime.parse("2026-10-08T10:00:00"), LocalDateTime.parse("2026-10-08T11:00:00"));
		em.persist(Warning.builder().userId(id).kind(WarningKind.NO_SHOW).createdAt(LocalDateTime.parse("2026-10-07T08:00:00")).build());
		em.flush();

		mvc.perform(delete("/api/v1/users/me").header("Authorization", token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
		em.flush();
		em.clear();

		assertThat(users.findById(id)).isEmpty();
		assertThat(em.find(Rental.class, reserved.getId())).isNull();
		assertThat(retained.findByStudentNumber("2499001")).singleElement().satisfies(w -> {
			assertThat(w.getKind()).isEqualTo(WarningKind.NO_SHOW);
			assertThat(w.getRetainUntil()).isEqualTo(LocalDateTime.parse("2027-04-07T10:00:00"));
		});
	}

	@Test
	void B1_09_2_탈퇴한_이메일로_다시_가입() throws Exception {
		mvc.perform(delete("/api/v1/users/me").header("Authorization", studentToken())).andExpect(status().isOk());
		em.flush();
		em.persist(EmailVerification.builder().email("demo@hansung.ac.kr").purpose(VerificationPurpose.SIGNUP)
			.codeHash(passwordEncoder.encode("483920")).expiresAt(LocalDateTime.parse("2026-10-07T10:05:00")).build());
		em.flush();

		mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"demo@hansung.ac.kr\",\"code\":\"483920\",\"password\":\"safePass123\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.user.approval").value("NONE"));
	}

	@Test
	void B1_09_3_탈퇴_전_refreshToken은_401() throws Exception {
		String refresh = tokenService.issue(users.findById(student()).orElseThrow()).refreshToken();
		mvc.perform(delete("/api/v1/users/me").header("Authorization", studentToken())).andExpect(status().isOk());
		em.flush();

		mvc.perform(post("/api/v1/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_09_관리자는_탈퇴_주소를_쓸_수_없음() throws Exception {
		mvc.perform(delete("/api/v1/users/me").header("Authorization", adminToken()))
			.andExpect(status().isForbidden());
		assertThat(users.findById(admin())).isPresent();
	}
}
