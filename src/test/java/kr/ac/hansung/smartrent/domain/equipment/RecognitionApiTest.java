package kr.ac.hansung.smartrent.domain.equipment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** B1-12 서버 구현 후 확인할 것 1~3행. 모델 key 48은 Insta360 EVO */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, RecognitionApiTest.Fakes.class})
class RecognitionApiTest {

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
	EquipmentModelRepository models;
	@Autowired
	JdbcTemplate jdbc;

	private ResultActions recognize(String body) throws Exception {
		String token = TestTokens.bearer(jwtEncoder, users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId(), "STUDENT",
			NOW.plusSeconds(1800));
		return mvc.perform(post("/api/v1/models/recognition").header("Authorization", token)
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private EquipmentModel insta() {
		return models.findAll().stream().filter(m -> m.getModelKey().equals("48")).findFirst().orElseThrow();
	}

	@Test
	void B1_12_1_대소문자와_공백이_달라도_모델_48_하나() throws Exception {
		long id = insta().getId();
		for (String name : new String[] {"Insta360 EVO", "insta360 evo", "Insta360EVO", "  INSTA360  EVO "}) {
			recognize("{\"modelName\":\"" + name + "\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id))
				.andExpect(jsonPath("$.data.name").value("Insta360 EVO"))
				.andExpect(jsonPath("$.data.source").value("SITE"))
				.andExpect(jsonPath("$.data.availableNow").isNumber());
		}
	}

	@Test
	void B1_12_2_없는_모델은_404() throws Exception {
		recognize("{\"modelName\":\"없는 모델\"}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	@Test
	void B1_12_3_운영_종료한_모델의_이름은_404() throws Exception {
		EquipmentModel m = insta();
		jdbc.update("UPDATE equipment_models SET archived = TRUE, name_key = NULL WHERE id = ?", m.getId());
		try {
			recognize("{\"modelName\":\"Insta360 EVO\"}").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		}
		finally {
			jdbc.update("UPDATE equipment_models SET archived = FALSE, name_key = ? WHERE id = ?",
				EquipmentModel.nameKeyOf(m.getName()), m.getId());
		}
	}

	@Test
	void B1_12_모델명이_비면_400() throws Exception {
		recognize("{\"modelName\":\"  \"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
