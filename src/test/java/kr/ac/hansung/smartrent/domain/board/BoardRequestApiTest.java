package kr.ac.hansung.smartrent.domain.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.board.repository.InquiryRepository;
import kr.ac.hansung.smartrent.domain.board.repository.PurchaseRequestRepository;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.port.BoardAlert;
import kr.ac.hansung.smartrent.support.MutableClock;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * B1-23 서버 구현 후 확인할 것 1~4행. 문의(inquiries)와 구매 요청(purchase-requests)을 같은 시험으로 돌립니다(4행).
 * 알림(B2-13 전)은 호출을 기록하는 가짜로 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, BoardRequestApiTest.Fakes.class})
class BoardRequestApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	static class RecordingBoardAlert implements BoardAlert {
		final List<String> sent = new ArrayList<>();

		@Override
		public void newRequest(String relatedType, Long relatedId, String title) {
			sent.add("REQUEST " + relatedType + " " + relatedId);
		}

		@Override
		public void answered(Long userId, String relatedType, Long relatedId, String title) {
			sent.add("RESPONSE " + userId + " " + relatedType + " " + relatedId);
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
		RecordingBoardAlert recordingBoardAlert() {
			return new RecordingBoardAlert();
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	MutableClock clock;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	InquiryRepository inquiries;
	@Autowired
	PurchaseRequestRepository purchases;
	@Autowired
	RecordingBoardAlert alert;
	@Autowired
	JsonMapper json;

	@BeforeEach
	void reset() {
		clock.set(NOW);
		inquiries.deleteAll();
		purchases.deleteAll();
		alert.sent.clear();
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, String email, String role) {
		return req.header("Authorization", TestTokens.bearer(jwtEncoder, users.findByEmail(email).orElseThrow().getId(), role,
			clock.instant().plusSeconds(1800)));
	}

	private JsonNode data(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
		String res = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("data");
	}

	private static String kindOf(String path) {
		return path.equals("inquiries") ? "INQUIRY" : "PURCHASE_REQUEST";
	}

	@ParameterizedTest
	@ValueSource(strings = {"inquiries", "purchase-requests"})
	void B1_23_1_2_4_김하늘이_쓰면_관리자에게_알리고_답하면_학생에게_알리며_답변_시각이_찬다(String path) throws Exception {
		Long demo = users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId();
		JsonNode written = data(as(post("/api/v1/" + path), "demo@hansung.ac.kr", "STUDENT").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"하루 더 쓸 수 있나요\",\"body\":\"내일 발표가 있어요.\"}"), 201);
		long id = written.get("id").asLong();
		assertThat(written.get("userName").asString()).isEqualTo("김하늘");
		assertThat(alert.sent).containsExactly("REQUEST " + kindOf(path) + " " + id);

		JsonNode mine = data(as(get("/api/v1/" + path + "/me"), "demo@hansung.ac.kr", "STUDENT"), 200);
		assertThat(mine).hasSize(1);
		assertThat(mine.get(0).get("response").isNull()).isTrue();
		assertThat(mine.get(0).get("respondedAt").isNull()).isTrue();

		clock.advance(Duration.ofHours(1));
		JsonNode answered = data(as(post("/api/v1/admin/" + path + "/" + id + "/answer"), "admin@hansung.ac.kr", "ADMIN")
			.contentType(MediaType.APPLICATION_JSON).content("{\"response\":\"대여 상세에서 연장해 주세요.\"}"), 200);
		assertThat(answered.get("response").asString()).isEqualTo("대여 상세에서 연장해 주세요.");
		assertThat(answered.get("respondedAt").asString()).startsWith("2026-10-07T11:00");
		assertThat(alert.sent).containsExactly("REQUEST " + kindOf(path) + " " + id, "RESPONSE " + demo + " " + kindOf(path) + " " + id);
		assertThat(data(as(get("/api/v1/" + path + "/me"), "demo@hansung.ac.kr", "STUDENT"), 200).get(0).get("response").asString())
			.isEqualTo("대여 상세에서 연장해 주세요.");
	}

	@ParameterizedTest
	@ValueSource(strings = {"inquiries", "purchase-requests"})
	void B1_23_3_4_정민준의_내_목록에는_김하늘의_글이_없다(String path) throws Exception {
		data(as(post("/api/v1/" + path), "demo@hansung.ac.kr", "STUDENT").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"김하늘의 글\",\"body\":\"내용\"}"), 201);
		assertThat(data(as(get("/api/v1/" + path + "/me"), "minjun@hansung.ac.kr", "STUDENT"), 200)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"inquiries", "purchase-requests"})
	void B1_23_4_관리자_목록은_답변_대기_먼저이고_answered로_거른다(String path) throws Exception {
		long first = data(as(post("/api/v1/" + path), "demo@hansung.ac.kr", "STUDENT").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"먼저 쓴 글\",\"body\":\"내용\"}"), 201).get("id").asLong();
		clock.advance(Duration.ofMinutes(10));
		data(as(post("/api/v1/" + path), "minjun@hansung.ac.kr", "STUDENT").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"나중에 쓴 글\",\"body\":\"내용\"}"), 201);
		clock.advance(Duration.ofMinutes(10));
		data(as(post("/api/v1/admin/" + path + "/" + first + "/answer"), "admin@hansung.ac.kr", "ADMIN")
			.contentType(MediaType.APPLICATION_JSON).content("{\"response\":\"답변\"}"), 200);
		JsonNode all = data(as(get("/api/v1/admin/" + path), "admin@hansung.ac.kr", "ADMIN"), 200);
		assertThat(all.get("items")).extracting(n -> n.get("title").asString()).containsExactly("나중에 쓴 글", "먼저 쓴 글");
		assertThat(all.get("items").get(0).get("userName").asString()).isEqualTo("정민준");
		assertThat(all.get("page").get("totalElements").asLong()).isEqualTo(2);
		assertThat(data(as(get("/api/v1/admin/" + path + "?answered=true"), "admin@hansung.ac.kr", "ADMIN"), 200).get("items"))
			.extracting(n -> n.get("title").asString()).containsExactly("먼저 쓴 글");
	}

	@ParameterizedTest
	@ValueSource(strings = {"inquiries", "purchase-requests"})
	void B1_23_권한과_오류(String path) throws Exception {
		mvc.perform(as(get("/api/v1/admin/" + path), "demo@hansung.ac.kr", "STUDENT")).andExpect(status().isForbidden());
		mvc.perform(as(post("/api/v1/" + path), "admin@hansung.ac.kr", "ADMIN").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"관리자\",\"body\":\"내용\"}")).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		mvc.perform(as(post("/api/v1/" + path), "demo@hansung.ac.kr", "STUDENT").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"\",\"body\":\"내용\"}")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(post("/api/v1/admin/" + path + "/999999/answer"), "admin@hansung.ac.kr", "ADMIN")
			.contentType(MediaType.APPLICATION_JSON).content("{\"response\":\"답변\"}")).andExpect(status().isNotFound());
	}
}
