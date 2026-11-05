package kr.ac.hansung.smartrent.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.recommendation.repository.RecommendationLogRepository;
import kr.ac.hansung.smartrent.domain.recommendation.service.RecommendationService;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** B1-21 서버 구현 후 확인할 것 6행: AI_ENABLED=false면 제공자를 부르지 않고 대체 추천 */
@SpringBootTest(properties = "smartrent.ai.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, RecommendationFallbackTest.Fakes.class})
class RecommendationFallbackTest {

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
	RecommendationLogRepository logs;

	@Test
	void B1_21_6_AI_ENABLED_false면_이름에_단어가_든_모델을_대체_추천하고_로그에_표시한다() throws Exception {
		String token = TestTokens.bearer(jwtEncoder, users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId(), "STUDENT",
			NOW.plusSeconds(1800));
		mvc.perform(post("/api/v1/recommendations").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"purpose\":\"insta 360 촬영\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].name").value("Insta360 EVO"))
			.andExpect(jsonPath("$.data.modelIdVerified").value(false));
		assertThat(logs.findAll().getLast().getModelIdUsed()).isEqualTo(RecommendationService.FALLBACK);
	}
}
