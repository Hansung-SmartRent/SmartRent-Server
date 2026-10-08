package kr.ac.hansung.smartrent.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.RequireApprovedStudent;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B1-04 서버 구현 후 확인할 것 9행: 승인 전 학생은 예약 같은 동작에서 NOT_APPROVED, 정지 학생은 SUSPENDED.
 * 예약 API(POST /reservations)는 B2-03에서 만들어지므로, 같은 표시(@RequireApprovedStudent)를 붙인 시험용 주소로 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, StudentAccessTest.FakeReservationController.class})
class StudentAccessTest {

	@RestController
	static class FakeReservationController {
		@RequireApprovedStudent
		@PostMapping("/api/v1/test-only/reservations")
		ApiResponse<Void> reserve() {
			return ApiResponse.ok(null);
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;

	private String tokenOf(String email) {
		return TestTokens.bearer(jwtEncoder, users.findByEmail(email).orElseThrow().getId(), "STUDENT");
	}

	@Test
	void B1_04_9_승인_대기_학생은_NOT_APPROVED() throws Exception {
		mvc.perform(post("/api/v1/test-only/reservations").header("Authorization", tokenOf("pending@hansung.ac.kr")))
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("NOT_APPROVED"));
	}

	@Test
	void B1_04_9_반려_미제출_학생도_NOT_APPROVED() throws Exception {
		mvc.perform(post("/api/v1/test-only/reservations").header("Authorization", tokenOf("yerin@hansung.ac.kr")))
			.andExpect(jsonPath("$.errorCode").value("NOT_APPROVED"));
		mvc.perform(post("/api/v1/test-only/reservations").header("Authorization", tokenOf("newstudent@hansung.ac.kr")))
			.andExpect(jsonPath("$.errorCode").value("NOT_APPROVED"));
	}

	@Test
	void B1_04_정지된_학생은_SUSPENDED() throws Exception {
		mvc.perform(post("/api/v1/test-only/reservations").header("Authorization", tokenOf("taeo@hansung.ac.kr")))
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("SUSPENDED"));
	}

	@Test
	void B1_04_승인되고_정지_아닌_학생은_통과() throws Exception {
		mvc.perform(post("/api/v1/test-only/reservations").header("Authorization", tokenOf("demo@hansung.ac.kr")))
			.andExpect(status().isOk());
	}
}
