package kr.ac.hansung.smartrent.domain.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.board.repository.NoticeRepository;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.port.NoticeAlert;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** B1-22 서버 구현 후 확인할 것 1~3행. 학생 알림(B2-13 전)은 호출을 기록하는 가짜로 확인 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, NoticeApiTest.Fakes.class})
class NoticeApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	static class RecordingNoticeAlert implements NoticeAlert {
		final List<String> sent = new ArrayList<>();

		@Override
		public void toStudents(Long noticeId, String title, String body) {
			sent.add(title);
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
		RecordingNoticeAlert recordingNoticeAlert() {
			return new RecordingNoticeAlert();
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
	NoticeRepository notices;
	@Autowired
	RecordingNoticeAlert alert;
	@Autowired
	JsonMapper json;

	@BeforeEach
	void reset() {
		clock.set(NOW);
		notices.deleteAll();
		alert.sent.clear();
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, String email, String role) {
		return req.header("Authorization", TestTokens.bearer(jwtEncoder, users.findByEmail(email).orElseThrow().getId(), role,
			clock.instant().plusSeconds(1800)));
	}

	private long write(String title, String body) throws Exception {
		String res = mvc.perform(as(post("/api/v1/admin/notices"), "admin@hansung.ac.kr", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("data").get("id").asLong();
	}

	private JsonNode list() throws Exception {
		String res = mvc.perform(as(get("/api/v1/notices"), "demo@hansung.ac.kr", "STUDENT")).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("data");
	}

	@Test
	void B1_22_1_관리자가_쓴_공지가_학생_목록_맨_위에_보이고_학생들에게_알린다() throws Exception {
		write("기자재 대여 이용 안내", "학생증을 지참해 주세요.");
		clock.advance(Duration.ofMinutes(5));
		write("추석 연휴 운영 안내", "9월 24일부터 26일까지 휴무입니다.");
		JsonNode data = list();
		assertThat(data.get("items")).hasSize(2);
		assertThat(data.get("items").get(0).get("title").asString()).isEqualTo("추석 연휴 운영 안내");
		assertThat(data.get("page").get("totalElements").asLong()).isEqualTo(2);
		assertThat(alert.sent).containsExactly("기자재 대여 이용 안내", "추석 연휴 운영 안내");
	}

	@Test
	void B1_22_2_학생_토큰으로_쓰면_403() throws Exception {
		mvc.perform(as(post("/api/v1/admin/notices"), "demo@hansung.ac.kr", "STUDENT")
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제목\",\"body\":\"내용\"}"))
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		assertThat(notices.count()).isZero();
	}

	@Test
	void B1_22_3_수정과_삭제가_목록에_반영된다() throws Exception {
		long id = write("처음 제목", "처음 내용");
		mvc.perform(as(put("/api/v1/admin/notices/" + id), "admin@hansung.ac.kr", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"고친 제목\",\"body\":\"고친 내용\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("고친 제목"));
		assertThat(list().get("items").get(0).get("body").asString()).isEqualTo("고친 내용");
		mvc.perform(as(delete("/api/v1/admin/notices/" + id), "admin@hansung.ac.kr", "ADMIN")).andExpect(status().isOk());
		assertThat(list().get("items")).isEmpty();
		mvc.perform(as(delete("/api/v1/admin/notices/" + id), "admin@hansung.ac.kr", "ADMIN")).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		assertThat(alert.sent).containsExactly("처음 제목");
	}

	@Test
	void B1_22_제목이_비면_400() throws Exception {
		mvc.perform(as(post("/api/v1/admin/notices"), "admin@hansung.ac.kr", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"body\":\"내용\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
