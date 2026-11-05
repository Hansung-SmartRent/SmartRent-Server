package kr.ac.hansung.smartrent.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.audit.AuditLog;
import kr.ac.hansung.smartrent.global.audit.AuditLogRepository;
import kr.ac.hansung.smartrent.support.MutableClock;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** B1-16 서버 구현 후 확인할 것 1·2행. 바꾼 김하늘의 이름·학번은 끝에 되돌립니다 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, AdminStudentPatchApiTest.Fakes.class})
class AdminStudentPatchApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	@TestConfiguration
	static class Fakes {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(NOW);
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	AuditLogRepository audits;
	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void restore() {
		jdbc.update("UPDATE users SET name = '김하늘', student_number = '2499001' WHERE email = 'demo@hansung.ac.kr'");
	}

	private User user(String email) {
		return users.findByEmail(email).orElseThrow();
	}

	private ResultActions patchStudent(String email, String role, Long id, String body) throws Exception {
		return mvc.perform(patch("/api/v1/admin/students/" + id)
			.header("Authorization", TestTokens.bearer(jwtEncoder, user(email).getId(), role, NOW.plusSeconds(1800)))
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void B1_16_1_이름을_김하나로_고치면_관리_기록에_전후_값이_남는다() throws Exception {
		Long id = user("demo@hansung.ac.kr").getId();
		long before = audits.count();
		patchStudent("admin@hansung.ac.kr", "ADMIN", id, "{\"name\":\"김하나\",\"studentNumber\":\"2499001\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("김하나"))
			.andExpect(jsonPath("$.data.studentNumber").value("2499001"));
		assertThat(user("demo@hansung.ac.kr").getName()).isEqualTo("김하나");
		assertThat(audits.count()).isEqualTo(before + 1);
		AuditLog log = audits.findAll().stream().max(java.util.Comparator.comparing(AuditLog::getId)).orElseThrow();
		assertThat(log.getAction()).isEqualTo("STUDENT_EDIT");
		assertThat(log.getDetail()).contains("김하늘").contains("김하나");
		assertThat(log.getActorId()).isEqualTo(user("admin@hansung.ac.kr").getId());
	}

	@Test
	void B1_16_2_정민준의_학번으로_고치면_409이고_그대로() throws Exception {
		Long id = user("demo@hansung.ac.kr").getId();
		long before = audits.count();
		patchStudent("admin@hansung.ac.kr", "ADMIN", id, "{\"name\":\"김하늘\",\"studentNumber\":\"2499005\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("STUDENT_NUMBER_DUPLICATE"));
		assertThat(user("demo@hansung.ac.kr").getStudentNumber()).isEqualTo("2499001");
		assertThat(audits.count()).isEqualTo(before);
	}

	@Test
	void B1_16_학생_토큰은_403이고_형식이_틀리면_400() throws Exception {
		Long id = user("demo@hansung.ac.kr").getId();
		patchStudent("demo@hansung.ac.kr", "STUDENT", id, "{\"name\":\"김하나\",\"studentNumber\":\"2499001\"}")
			.andExpect(status().isForbidden());
		patchStudent("admin@hansung.ac.kr", "ADMIN", id, "{\"name\":\"\",\"studentNumber\":\"12\"}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		patchStudent("admin@hansung.ac.kr", "ADMIN", 999999L, "{\"name\":\"김하나\",\"studentNumber\":\"2499001\"}")
			.andExpect(status().isNotFound());
	}
}
