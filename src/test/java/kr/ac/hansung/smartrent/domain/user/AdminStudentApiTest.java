package kr.ac.hansung.smartrent.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.port.ActiveWarningCounter;
import kr.ac.hansung.smartrent.support.FlakyStorage;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * B1-14 서버 구현 후 확인할 것 1~5행. 경고 표(B2-12) 전이라 유효 경고 수는 시험용 가짜(FakeWarnings)로 정합니다.
 * 처음 데이터의 학생: 김하늘(승인), 이새봄(대기), 정민준(승인), 윤태오(정지), 강예린(반려), 새 학생(미제출).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, AdminStudentApiTest.Fakes.class})
class AdminStudentApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	static class FakeWarnings implements ActiveWarningCounter {
		final Map<Long, Long> counts = new HashMap<>();

		@Override
		public long count(Long userId) {
			return counts.getOrDefault(userId, 0L);
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
		FlakyStorage flakyStorage(MutableClock clock) {
			return new FlakyStorage(clock);
		}

		@Bean
		@Primary
		FakeWarnings fakeWarnings() {
			return new FakeWarnings();
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	FakeWarnings warnings;
	@Autowired
	FlakyStorage storage;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	JsonMapper json;

	@BeforeEach
	void reset() {
		warnings.counts.clear();
	}

	private User user(String email) {
		return users.findByEmail(email).orElseThrow();
	}

	private ResultActions call(String email, String role, String url) throws Exception {
		return mvc.perform(get(url).header("Authorization", TestTokens.bearer(jwtEncoder, user(email).getId(), role, NOW.plusSeconds(1800))));
	}

	private JsonNode admin(String url) throws Exception {
		String body = call("admin@hansung.ac.kr", "ADMIN", url).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}

	@Test
	void B1_14_1_승인_대기로_거르면_이새봄이_있고_승인된_학생은_없다() throws Exception {
		JsonNode data = admin("/api/v1/admin/students?approval=PENDING");
		assertThat(data.get("items")).extracting(n -> n.get("name").asString()).contains("이새봄");
		assertThat(data.get("items")).extracting(n -> n.get("approval").asString()).containsOnly("PENDING");
		assertThat(data.get("page").get("page").asInt()).isZero();
		assertThat(data.get("page").get("size").asInt()).isEqualTo(20);
	}

	@Test
	void B1_14_1_관리자_계정은_학생_목록에_없다() throws Exception {
		JsonNode items = admin("/api/v1/admin/students").get("items");
		assertThat(items).hasSize(6);
		assertThat(items).extracting(n -> n.get("email").asString()).doesNotContain("admin@hansung.ac.kr");
	}

	@Test
	void B1_14_2_학번으로_찾으면_김하늘_하나() throws Exception {
		JsonNode items = admin("/api/v1/admin/students?q=2499001").get("items");
		assertThat(items).hasSize(1);
		assertThat(items.get(0).get("name").asString()).isEqualTo("김하늘");
		assertThat(admin("/api/v1/admin/students?q=MINJUN").get("items")).extracting(n -> n.get("name").asString())
			.containsExactly("정민준");
	}

	@Test
	void B1_14_3_학생_토큰으로는_403() throws Exception {
		call("demo@hansung.ac.kr", "STUDENT", "/api/v1/admin/students").andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		call("demo@hansung.ac.kr", "STUDENT", "/api/v1/admin/students/" + user("pending@hansung.ac.kr").getId())
			.andExpect(status().isForbidden());
	}

	@Test
	void B1_14_4_승인_대기_학생만_학생증_사진_주소가_있다() throws Exception {
		User pending = user("pending@hansung.ac.kr");
		String key = "student-id/b1-14-test.png";
		storage.put(key, new byte[] {1, 2, 3}, "image/png");
		jdbc.update("UPDATE users SET student_id_image_key = ? WHERE id = ?", key, pending.getId());
		try {
			JsonNode detail = admin("/api/v1/admin/students/" + pending.getId());
			assertThat(detail.get("studentIdImageUrl").isString()).isTrue();
			assertThat(detail.get("hasStudentIdImage").asBoolean()).isTrue();
			assertThat(detail.get("warnings").isArray()).isTrue();
			assertThat(detail.get("rentals").isArray()).isTrue();
			JsonNode approved = admin("/api/v1/admin/students/" + user("demo@hansung.ac.kr").getId());
			assertThat(approved.get("studentIdImageUrl").isNull()).isTrue();
			assertThat(approved.get("name").asString()).isEqualTo("김하늘");
		}
		finally {
			jdbc.update("UPDATE users SET student_id_image_key = NULL WHERE id = ?", pending.getId());
		}
	}

	@Test
	void B1_14_4_없는_학생이나_관리자_id는_404() throws Exception {
		call("admin@hansung.ac.kr", "ADMIN", "/api/v1/admin/students/999999").andExpect(status().isNotFound());
		call("admin@hansung.ac.kr", "ADMIN", "/api/v1/admin/students/" + user("admin@hansung.ac.kr").getId())
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	@Test
	void B1_14_5_유효_경고가_있고_정지되지_않은_학생만_거른_뒤_쪽을_나눈다() throws Exception {
		warnings.counts.put(user("demo@hansung.ac.kr").getId(), 1L);
		warnings.counts.put(user("minjun@hansung.ac.kr").getId(), 2L);
		warnings.counts.put(user("taeo@hansung.ac.kr").getId(), 3L); // 정지 중이라 빠져야 함
		JsonNode first = admin("/api/v1/admin/students?hasWarnings=true&suspended=false&page=0&size=1");
		assertThat(first.get("items")).hasSize(1);
		assertThat(first.get("page").get("totalElements").asLong()).isEqualTo(2);
		assertThat(first.get("page").get("totalPages").asLong()).isEqualTo(2);
		JsonNode second = admin("/api/v1/admin/students?hasWarnings=true&suspended=false&page=1&size=1");
		assertThat(second.get("items")).hasSize(1);
		assertThat(java.util.List.of(first.get("items").get(0).get("name").asString(), second.get("items").get(0).get("name").asString()))
			.containsExactlyInAnyOrder("김하늘", "정민준");
		JsonNode suspended = admin("/api/v1/admin/students?suspended=true").get("items");
		assertThat(suspended).extracting(n -> n.get("name").asString()).containsExactly("윤태오");
		assertThat(suspended.get(0).get("suspension").get("suspended").asBoolean()).isTrue();
	}

	@Test
	void B1_14_5_size가_100을_넘으면_400() throws Exception {
		call("admin@hansung.ac.kr", "ADMIN", "/api/v1/admin/students?size=101").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
