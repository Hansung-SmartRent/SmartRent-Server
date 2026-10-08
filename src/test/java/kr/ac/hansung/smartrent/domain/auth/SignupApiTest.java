package kr.ac.hansung.smartrent.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.auth.service.VerificationCodeGenerator;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.mail.CodeMailer;
import kr.ac.hansung.smartrent.support.FakeMailer;
import kr.ac.hansung.smartrent.support.MutableClock;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** B1-05 서버 구현 후 확인할 것 1~7행(사례 AUTH-01, AUTH-02). 메일은 시험용 FakeMailer, 인증번호는 사례의 483920 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, SignupApiTest.Fakes.class})
class SignupApiTest {

	static final Instant SENT_AT = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	@TestConfiguration
	static class Fakes {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(SENT_AT);
		}

		@Bean
		@Primary
		CodeMailer fakeMailer() {
			return new FakeMailer();
		}

		@Bean
		@Primary
		CaseCode caseCode() {
			return new CaseCode();
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	MutableClock clock;
	@Autowired
	CodeMailer mailer;
	@Autowired
	UserRepository users;
	@Autowired
	CaseCode code;

	/** 시험용 인증번호: 기본은 사례 AUTH-02의 483920, 필요하면 바꿔 씀 */
	static class CaseCode extends VerificationCodeGenerator {
		String value = "483920";

		@Override
		public String next() {
			return value;
		}
	}

	@BeforeEach
	void reset() {
		clock.set(SENT_AT);
		((FakeMailer) mailer).fail = false;
		code.value = "483920";
	}

	private ResultActions sendCode(String email, String purpose) throws Exception {
		return mvc.perform(post("/api/v1/auth/email-codes").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"purpose\":\"" + purpose + "\"}"));
	}

	private ResultActions signup(String email, String code, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\",\"password\":\"" + password + "\"}"));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	@Test
	void B1_05_1_AUTH_01_허용_도메인만_통과하고_형식_오류는_VALIDATION_ERROR() throws Exception {
		for (String allowed : new String[] {"demo@hansung.ac.kr", "DEMO@Hansung.AC.KR", "a1@hansung.kr", "a1@hansung.edu"}) {
			String code = String.valueOf(JsonPathRead.errorCode(sendCode(allowed, "SIGNUP")));
			assertThat(code).as(allowed).isNotEqualTo("EMAIL_DOMAIN_NOT_ALLOWED").isNotEqualTo("VALIDATION_ERROR");
		}
		for (String denied : new String[] {"a@myhansung.ac.kr", "a@hansung.ac.kr.example.com", "a@gmail.com"}) {
			sendCode(denied, "SIGNUP").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("EMAIL_DOMAIN_NOT_ALLOWED"));
		}
		sendCode("@hansung.ac.kr", "SIGNUP").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void B1_05_2_AUTH_02_5분_안은_성공_지나면_OTP_EXPIRED_틀리면_OTP_INVALID() throws Exception {
		sendCode("auth02-a@hansung.ac.kr", "SIGNUP").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.expiresAt").value("2026-10-07T10:05:00+09:00"));
		clock.set(OffsetDateTime.parse("2026-10-07T10:04:59+09:00").toInstant());
		signup("auth02-a@hansung.ac.kr", "483920", "safePass123").andExpect(status().isCreated());

		clock.set(SENT_AT);
		sendCode("auth02-b@hansung.ac.kr", "SIGNUP").andExpect(status().isOk());
		clock.set(OffsetDateTime.parse("2026-10-07T10:05:01+09:00").toInstant());
		signup("auth02-b@hansung.ac.kr", "483920", "safePass123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("OTP_EXPIRED"));

		clock.set(SENT_AT);
		sendCode("auth02-c@hansung.ac.kr", "SIGNUP").andExpect(status().isOk());
		clock.set(OffsetDateTime.parse("2026-10-07T10:01:00+09:00").toInstant());
		signup("auth02-c@hansung.ac.kr", "111111", "safePass123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("OTP_INVALID"));
	}

	@Test
	void B1_05_3_새_주소로_가입하면_NONE_소문자_이메일_토큰() throws Exception {
		sendCode("New.Student3@Hansung.ac.kr", "SIGNUP").andExpect(status().isOk());
		signup("New.Student3@Hansung.ac.kr", "483920", "safePass123").andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.user.approval").value("NONE"));
		User saved = users.findByEmail("new.student3@hansung.ac.kr").orElseThrow();
		assertThat(saved.getApproval()).isEqualTo(Approval.NONE);
		assertThat(saved.getPasswordHash()).startsWith("$2");
	}

	@Test
	void B1_05_4_이미_가입된_주소로_SIGNUP_번호를_요청하면_409() throws Exception {
		sendCode("demo@hansung.ac.kr", "SIGNUP").andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_USED"));
	}

	@Test
	void B1_05_5_번호를_두_번_받으면_첫_번호는_OTP_INVALID() throws Exception {
		code.value = "123456";
		sendCode("twice@hansung.ac.kr", "SIGNUP").andExpect(status().isOk());
		code.value = "483920";
		sendCode("twice@hansung.ac.kr", "SIGNUP").andExpect(status().isOk());

		signup("twice@hansung.ac.kr", "123456", "safePass123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("OTP_INVALID"));
		signup("twice@hansung.ac.kr", "483920", "safePass123").andExpect(status().isCreated());
	}

	@Test
	void B1_05_6_메일_보내기가_실패하면_503이고_그_번호로_가입할_수_없다() throws Exception {
		((FakeMailer) mailer).fail = true;
		sendCode("mailfail@hansung.ac.kr", "SIGNUP").andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.errorCode").value("MAIL_UNAVAILABLE"));
		((FakeMailer) mailer).fail = false;
		signup("mailfail@hansung.ac.kr", "483920", "safePass123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("OTP_INVALID"));
	}

	@Test
	void B1_05_7_비밀번호_찾기_뒤_새_비밀번호로_로그인되고_옛_비밀번호는_401() throws Exception {
		sendCode("minjun@hansung.ac.kr", "PASSWORD_RESET").andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"minjun@hansung.ac.kr\",\"code\":\"483920\",\"newPassword\":\"newPass1234\"}"))
			.andExpect(status().isOk());

		login("minjun@hansung.ac.kr", "newPass1234").andExpect(status().isOk());
		login("minjun@hansung.ac.kr", "demo1234").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
	}

	@Test
	void B1_05_가입되지_않은_주소로_비밀번호_찾기_번호를_요청하면_404() throws Exception {
		sendCode("nobody@hansung.ac.kr", "PASSWORD_RESET").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	@Test
	void B1_05_이메일_변경_번호는_로그인해야_받을_수_있다() throws Exception {
		sendCode("change@hansung.ac.kr", "EMAIL_CHANGE").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_05_비밀번호가_8자보다_짧으면_VALIDATION_ERROR() throws Exception {
		signup("short@hansung.ac.kr", "483920", "short").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	static final class JsonPathRead {
		static Object errorCode(ResultActions r) throws Exception {
			return com.jayway.jsonpath.JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.errorCode");
		}
	}
}
