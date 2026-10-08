package kr.ac.hansung.smartrent.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.auth.repository.RefreshTokenRepository;
import kr.ac.hansung.smartrent.domain.auth.service.VerificationCodeGenerator;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.mail.CodeMailer;
import kr.ac.hansung.smartrent.support.FakeMailer;
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

/** B1-06 서버 구현 후 확인할 것 1~6행. 메일은 시험용 FakeMailer, 인증번호는 483920 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, MyInfoApiTest.Fakes.class})
class MyInfoApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	@TestConfiguration
	static class Fakes {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(NOW);
		}

		@Bean
		@Primary
		CodeMailer fakeMailer() {
			return new FakeMailer();
		}

		@Bean
		@Primary
		VerificationCodeGenerator fixedCode() {
			return new VerificationCodeGenerator() {
				@Override
				public String next() {
					return "483920";
				}
			};
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
	RefreshTokenRepository refreshTokens;

	@BeforeEach
	void reset() {
		clock.set(NOW);
	}

	private String tokenOf(String email) {
		return TestTokens.bearer(jwtEncoder, users.findByEmail(email).orElseThrow().getId(), "STUDENT", NOW.plusSeconds(1800));
	}

	private ResultActions sendCode(String token, String email, String purpose) throws Exception {
		return mvc.perform(post("/api/v1/auth/email-codes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"purpose\":\"" + purpose + "\"}"));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	@Test
	void B1_06_1_내_정보() throws Exception {
		mvc.perform(get("/api/v1/users/me").header("Authorization", tokenOf("demo@hansung.ac.kr")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("김하늘"))
			.andExpect(jsonPath("$.data.studentNumber").value("2499001"))
			.andExpect(jsonPath("$.data.approval").value("APPROVED"))
			.andExpect(jsonPath("$.data.activeWarningCount").value(0))
			.andExpect(jsonPath("$.data.suspension.suspended").value(false));
	}

	@Test
	void B1_06_2_현재_이메일_번호로_비밀번호를_바꾸면_새_비밀번호로_로그인되고_리프레시_토큰이_모두_지워진다() throws Exception {
		login("minjun@hansung.ac.kr", "demo1234").andExpect(status().isOk());
		Long id = users.findByEmail("minjun@hansung.ac.kr").orElseThrow().getId();
		assertThat(refreshTokens.findAll()).anyMatch(t -> t.getUserId().equals(id));

		String token = tokenOf("minjun@hansung.ac.kr");
		sendCode(token, "minjun@hansung.ac.kr", "PASSWORD_RESET").andExpect(status().isOk());
		mvc.perform(put("/api/v1/users/me/password").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"483920\",\"newPassword\":\"changed123\"}"))
			.andExpect(status().isOk());

		assertThat(refreshTokens.findAll()).noneMatch(t -> t.getUserId().equals(id));
		login("minjun@hansung.ac.kr", "changed123").andExpect(status().isOk());
	}

	@Test
	void B1_06_3_틀린_번호로_비밀번호를_바꾸면_OTP_INVALID() throws Exception {
		String token = tokenOf("pending@hansung.ac.kr");
		sendCode(token, "pending@hansung.ac.kr", "PASSWORD_RESET").andExpect(status().isOk());
		mvc.perform(put("/api/v1/users/me/password").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"111111\",\"newPassword\":\"changed123\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("OTP_INVALID"));
	}

	@Test
	void B1_06_만료된_번호로_비밀번호를_바꾸면_OTP_EXPIRED() throws Exception {
		String token = tokenOf("yerin@hansung.ac.kr");
		sendCode(token, "yerin@hansung.ac.kr", "PASSWORD_RESET").andExpect(status().isOk());
		clock.set(NOW.plusSeconds(301));
		mvc.perform(put("/api/v1/users/me/password").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"483920\",\"newPassword\":\"changed123\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("OTP_EXPIRED"));
	}

	@Test
	void B1_06_4_새_학교_메일_번호로_이메일을_바꾸면_응답에_새_주소() throws Exception {
		String token = tokenOf("demo@hansung.ac.kr");
		sendCode(token, "kim.haneul@hansung.kr", "EMAIL_CHANGE").andExpect(status().isOk());
		mvc.perform(put("/api/v1/users/me/email").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"newEmail\":\"kim.haneul@hansung.kr\",\"code\":\"483920\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("kim.haneul@hansung.kr"));
		var changed = users.findByEmail("kim.haneul@hansung.kr").orElseThrow();
		// 다른 시험이 김하늘 계정을 원래 주소로 찾으므로 되돌림
		changed.changeEmail("demo@hansung.ac.kr");
		users.save(changed);
	}

	@Test
	void B1_06_5_새_이메일이_gmail이면_EMAIL_DOMAIN_NOT_ALLOWED() throws Exception {
		mvc.perform(put("/api/v1/users/me/email").header("Authorization", tokenOf("taeo@hansung.ac.kr"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"newEmail\":\"taeo@gmail.com\",\"code\":\"483920\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("EMAIL_DOMAIN_NOT_ALLOWED"));
	}

	@Test
	void B1_06_6_다른_사람이_쓰는_주소로는_바꿀_수_없다() throws Exception {
		mvc.perform(put("/api/v1/users/me/email").header("Authorization", tokenOf("taeo@hansung.ac.kr"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"newEmail\":\"minjun@hansung.ac.kr\",\"code\":\"483920\"}"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_USED"));
	}

	@Test
	void B1_06_로그인_없이_내_정보는_401() throws Exception {
		mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
	}
}
