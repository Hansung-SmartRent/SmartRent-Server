package kr.ac.hansung.smartrent.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.recommendation.client.AiClient;
import kr.ac.hansung.smartrent.domain.recommendation.entity.RecommendationLog;
import kr.ac.hansung.smartrent.domain.recommendation.repository.RecommendationLogRepository;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.support.MutableClock;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * B1-21 서버 구현 후 확인할 것 2~5·7·8행. 제공자는 시험용 가짜(FakeAi)로 바꿉니다.
 * 실제 Gemini 호출(1행)은 키 발급 뒤 실제 환경 확인으로 따로 기록합니다.
 */
@SpringBootTest(properties = {"smartrent.ai.enabled=true", "smartrent.ai.api-key=test-only-key",
	"smartrent.ai.model-id=test-model"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, RecommendationApiTest.Fakes.class})
class RecommendationApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:05:00+09:00").toInstant();
	static final String PURPOSE = "동아리 행사 영상을 360도로 찍고 싶어요";

	/** 정해 둔 답을 돌려주고, 받은 지시문과 호출 횟수를 기록하는 가짜 제공자 */
	static class FakeAi implements AiClient {
		Supplier<String> answer = () -> "{\"items\":[]}";
		final List<String> prompts = new ArrayList<>();

		@Override
		public String generate(String prompt) {
			prompts.add(prompt);
			return answer.get();
		}
	}

	@TestConfiguration
	static class Fakes {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(NOW);
		}

		@Bean
		@Primary
		FakeAi fakeAi() {
			return new FakeAi();
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
	RecommendationLogRepository logs;
	@Autowired
	FakeAi ai;

	@BeforeEach
	void reset() {
		ai.prompts.clear();
		ai.answer = () -> "{\"items\":[]}";
	}

	private ResultActions recommend(String purpose) throws Exception {
		String token = TestTokens.bearer(jwtEncoder, users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId(), "STUDENT",
			NOW.plusSeconds(1800));
		return mvc.perform(post("/api/v1/recommendations").header("Authorization", token)
			.contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"" + purpose + "\"}"));
	}

	private EquipmentModel insta() {
		return models.findAll().stream().filter(m -> m.getModelKey().equals("48")).findFirst().orElseThrow();
	}

	@Test
	void B1_21_2_목록에_없는_modelId는_버리고_48만_돌려준다() throws Exception {
		long id = insta().getId();
		ai.answer = () -> "{\"items\":[{\"modelId\":" + id + ",\"reason\":\"360도 영상을 한 번에 찍을 수 있어요.\"},"
			+ "{\"modelId\":9999,\"reason\":\"없는 모델\"}]}";
		recommend(PURPOSE).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items.length()").value(1))
			.andExpect(jsonPath("$.data.items[0].modelId").value(id))
			.andExpect(jsonPath("$.data.items[0].name").value("Insta360 EVO"))
			.andExpect(jsonPath("$.data.items[0].reason").value("360도 영상을 한 번에 찍을 수 있어요."))
			.andExpect(jsonPath("$.data.modelIdVerified").value(false));
	}

	@Test
	void B1_21_2_코드_울타리로_감싼_JSON도_읽는다() throws Exception {
		long id = insta().getId();
		ai.answer = () -> "```json\n{\"items\":[{\"modelId\":" + id + ",\"reason\":\"좋아요\"}]}\n```";
		recommend(PURPOSE).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].modelId").value(id));
	}

	@Test
	void B1_21_3_사용_목적_201자는_400() throws Exception {
		recommend("가".repeat(201)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		recommend("가".repeat(200)).andExpect(status().isOk());
		assertThat(ai.prompts).hasSize(1);
	}

	@Test
	void B1_21_4_제공자_실패는_503이고_다시_부르지_않는다() throws Exception {
		ai.answer = () -> {
			throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
		};
		recommend(PURPOSE).andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"));
		assertThat(ai.prompts).hasSize(1);
	}

	@Test
	void B1_21_4_JSON이_아닌_답은_503() throws Exception {
		ai.answer = () -> "추천할게요: Insta360 EVO";
		recommend(PURPOSE).andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"));
		ai.answer = () -> "{\"result\":\"items 없음\"}";
		recommend(PURPOSE).andExpect(status().isServiceUnavailable());
		assertThat(ai.prompts).hasSize(2);
	}

	@Test
	void B1_21_5_정상_호출_한_번에_recommendation_logs_1행() throws Exception {
		long before = logs.count();
		long id = insta().getId();
		ai.answer = () -> "{\"items\":[{\"modelId\":" + id + ",\"reason\":\"좋아요\"}]}";
		recommend(PURPOSE).andExpect(status().isOk());
		assertThat(logs.count()).isEqualTo(before + 1);
		RecommendationLog last = logs.findAll().getLast();
		assertThat(last.getPurpose()).isEqualTo(PURPOSE);
		assertThat(last.getModelIdUsed()).isEqualTo("test-model");
		assertThat(last.getResultJson()).contains("Insta360 EVO");
		assertThat(last.getUserId()).isEqualTo(users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId());
	}

	@Test
	void B1_21_5_실패하면_기록을_남기지_않는다() throws Exception {
		long before = logs.count();
		ai.answer = () -> "JSON 아님";
		recommend(PURPOSE).andExpect(status().isServiceUnavailable());
		assertThat(logs.count()).isEqualTo(before);
	}

	@Test
	void B1_21_8_제공자에게는_기자재_목록과_사용_목적만_보낸다() throws Exception {
		recommend(PURPOSE).andExpect(status().isOk());
		String prompt = ai.prompts.getFirst();
		User demo = users.findByEmail("demo@hansung.ac.kr").orElseThrow();
		assertThat(prompt).contains(PURPOSE).contains("Insta360 EVO").contains("\"modelId\":" + insta().getId());
		assertThat(prompt).doesNotContain(demo.getEmail()).doesNotContain(demo.getName())
			.doesNotContain(demo.getStudentNumber()).doesNotContain("password").doesNotContain("demo1234");
		// 대여 가능 여부(대수)는 넣지 않음(기획안 10장)
		assertThat(prompt).doesNotContain("availableNow").doesNotContain("normalUnits");
	}

	@Test
	void B1_21_로그인하지_않으면_401() throws Exception {
		mvc.perform(post("/api/v1/recommendations").contentType(MediaType.APPLICATION_JSON)
			.content("{\"purpose\":\"" + PURPOSE + "\"}")).andExpect(status().isUnauthorized());
	}
}
