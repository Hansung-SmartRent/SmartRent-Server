package kr.ac.hansung.smartrent.global.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import kr.ac.hansung.smartrent.support.TestTokens;

/** B1-01 서버 구현 후 확인할 것 1·2·5행 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ServerBasicsTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtEncoder jwtEncoder;

	@Test
	void B1_01_1_헬스체크는_200과_success_true() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.errorCode").isEmpty());
	}

	@Test
	void B1_01_2_없는_주소는_404_NOT_FOUND() throws Exception {
		// B1-04부터 로그인이 필요한 주소는 토큰 검사가 먼저라, 로그인한 사용자로 부름
		mockMvc.perform(get("/api/v1/없는주소").header("Authorization", TestTokens.bearer(jwtEncoder, 1, "STUDENT")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void B1_01_5_Swagger_화면이_열린다() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/health']").exists());
	}
}
