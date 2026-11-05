package kr.ac.hansung.smartrent.domain.equipment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.support.MutableClock;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** B1-11 서버 구현 후 확인할 것 1~3행. local 처음 데이터의 기기 코드 278은 Insta360 EVO(모델 key 48)의 기기 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, UnitLookupApiTest.Fakes.class})
class UnitLookupApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:05:00+09:00").toInstant();

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
	JdbcTemplate jdbc;

	private ResultActions scan(String email, String role, String code) throws Exception {
		String token = TestTokens.bearer(jwtEncoder, users.findByEmail(email).orElseThrow().getId(), role, NOW.plusSeconds(1800));
		return mvc.perform(get("/api/v1/units/by-code/" + code).header("Authorization", token));
	}

	@Test
	void B1_11_1_학생이_코드_278을_찍으면_Insta360_EVO_모델만_받는다() throws Exception {
		scan("demo@hansung.ac.kr", "STUDENT", "278").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.model.name").value("Insta360 EVO"))
			.andExpect(jsonPath("$.data.model.source").value("SITE"))
			.andExpect(jsonPath("$.data.unit").doesNotExist());
	}

	@Test
	void B1_11_1_관리자는_기기_상태까지_받는다() throws Exception {
		scan("admin@hansung.ac.kr", "ADMIN", "278").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.model.name").value("Insta360 EVO"))
			.andExpect(jsonPath("$.data.unit.code").value("278"))
			.andExpect(jsonPath("$.data.unit.condition").value("NORMAL"));
	}

	@Test
	void B1_11_2_없는_코드는_404() throws Exception {
		scan("demo@hansung.ac.kr", "STUDENT", "no-such-code").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	@Test
	void B1_11_3_지운_기기의_코드는_404() throws Exception {
		jdbc.update("UPDATE equipment_units SET deleted_at = '2026-10-07 09:00:00' WHERE code = '278'");
		try {
			scan("admin@hansung.ac.kr", "ADMIN", "278").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		}
		finally {
			jdbc.update("UPDATE equipment_units SET deleted_at = NULL WHERE code = '278'");
		}
	}

	@Test
	void B1_11_로그인하지_않으면_401() throws Exception {
		mvc.perform(get("/api/v1/units/by-code/278")).andExpect(status().isUnauthorized());
	}
}
