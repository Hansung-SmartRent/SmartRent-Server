package kr.ac.hansung.smartrent.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

import com.jayway.jsonpath.JsonPath;
import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.auth.repository.LoginFailureRepository;
import kr.ac.hansung.smartrent.domain.auth.repository.RefreshTokenRepository;
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

/** B1-04 서버 구현 후 확인할 것 1~8·10·11행. 계정은 fixtures(local 프로필 로더) */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, AuthApiTest.Clocks.class})
class AuthApiTest {

	static final Instant START = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	@TestConfiguration
	static class Clocks {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(START);
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	MutableClock clock;
	@Autowired
	LoginFailureRepository failures;
	@Autowired
	RefreshTokenRepository refreshTokens;
	@Autowired
	JwtEncoder jwtEncoder;

	@BeforeEach
	void reset() {
		clock.set(START);
		failures.deleteAll();
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	@Test
	void B1_04_1_김하늘_로그인하면_토큰과_APPROVED() throws Exception {
		login("demo@hansung.ac.kr", "demo1234").andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
			.andExpect(jsonPath("$.data.user.approval").value("APPROVED"))
			.andExpect(jsonPath("$.data.user.suspension.suspended").value(false));
	}

	@Test
	void B1_04_2_승인_대기_이새봄도_로그인은_되고_PENDING() throws Exception {
		login("pending@hansung.ac.kr", "demo1234").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.approval").value("PENDING"));
	}

	@Test
	void B1_04_3_정지된_윤태오는_응답에_정지_정보가_있다() throws Exception {
		login("taeo@hansung.ac.kr", "demo1234").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.suspension.suspended").value(true))
			.andExpect(jsonPath("$.data.user.suspension.until").isNotEmpty());
	}

	@Test
	void B1_04_완료조건_7개_계정이_각자_상태대로_로그인된다() throws Exception {
		login("minjun@hansung.ac.kr", "demo1234").andExpect(jsonPath("$.data.user.approval").value("APPROVED"));
		login("yerin@hansung.ac.kr", "demo1234").andExpect(jsonPath("$.data.user.approval").value("REJECTED"))
			.andExpect(jsonPath("$.data.user.rejectReason").isNotEmpty());
		login("newstudent@hansung.ac.kr", "demo1234").andExpect(jsonPath("$.data.user.approval").value("NONE"));
		login("admin@hansung.ac.kr", "demo1234").andExpect(jsonPath("$.data.user.role").value("ADMIN"));
	}

	@Test
	void B1_04_4_5_6_다섯_번_틀리면_막히고_5분_뒤_풀린다() throws Exception {
		for (int i = 0; i < 5; i++) {
			login("demo@hansung.ac.kr", "wrong-password").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
		}
		login("demo@hansung.ac.kr", "demo1234").andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.errorCode").value("LOGIN_LOCKED"));

		clock.advance(Duration.ofMinutes(5));
		login("demo@hansung.ac.kr", "demo1234").andExpect(status().isOk());
		assertThat(failures.findById("demo@hansung.ac.kr")).isEmpty();
	}

	@Test
	void B1_04_없는_이메일도_같은_INVALID_CREDENTIALS() throws Exception {
		login("nobody@hansung.ac.kr", "demo1234").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
	}

	@Test
	void B1_04_7_토큰_없이_로그인_필요한_API를_부르면_401() throws Exception {
		mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_04_만료되거나_위조된_토큰은_401() throws Exception {
		mvc.perform(get("/api/v1/users/me").header("Authorization",
				TestTokens.bearer(jwtEncoder, 1, "STUDENT", START.minusSeconds(120))))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer abc.def.ghi"))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_04_8_학생_토큰으로_관리자_API를_부르면_403() throws Exception {
		String token = JsonPath.read(login("demo@hansung.ac.kr", "demo1234").andReturn().getResponse().getContentAsString(),
			"$.data.accessToken");
		mvc.perform(get("/api/v1/admin/summary").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
	}

	@Test
	void B1_04_10_리프레시_토큰으로_새_토큰을_받고_옛_리프레시_토큰은_못_쓴다() throws Exception {
		String body = login("demo@hansung.ac.kr", "demo1234").andReturn().getResponse().getContentAsString();
		String refresh = JsonPath.read(body, "$.data.refreshToken");
		String expiredAccess = TestTokens.bearer(jwtEncoder, 1, "STUDENT", START.minusSeconds(120));

		// 만료된 액세스 토큰이 헤더에 붙어 와도 토큰 새로 받기는 된다
		String renewed = mvc.perform(post("/api/v1/auth/token/refresh").header("Authorization", expiredAccess)
				.contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andReturn().getResponse().getContentAsString();
		assertThat((String) JsonPath.read(renewed, "$.data.refreshToken")).isNotEqualTo(refresh);

		mvc.perform(post("/api/v1/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}"))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_04_11_로그아웃한_리프레시_토큰으로는_새로_받을_수_없다() throws Exception {
		String body = login("demo@hansung.ac.kr", "demo1234").andReturn().getResponse().getContentAsString();
		String access = JsonPath.read(body, "$.data.accessToken");
		String refresh = JsonPath.read(body, "$.data.refreshToken");

		mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\",\"pushToken\":\"device-token-1\"}"))
			.andExpect(status().isOk());

		mvc.perform(post("/api/v1/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}"))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_04_리프레시_토큰은_원문이_아니라_해시로_저장된다() throws Exception {
		String refresh = JsonPath.read(login("demo@hansung.ac.kr", "demo1234").andReturn().getResponse().getContentAsString(),
			"$.data.refreshToken");
		assertThat(refreshTokens.findAll()).allSatisfy(t -> assertThat(t.getTokenHash()).isNotEqualTo(refresh).hasSize(64));
	}

	@Test
	void B1_04_로그인_입력이_비면_400_VALIDATION_ERROR() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
