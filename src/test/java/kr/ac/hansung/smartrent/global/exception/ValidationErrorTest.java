package kr.ac.hansung.smartrent.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** B1-01 서버 구현 후 확인할 것 3행: 검증 오류는 400 VALIDATION_ERROR, data에 칸별 오류 */
class ValidationErrorTest {

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void B1_01_3_필수_칸을_빼면_400_VALIDATION_ERROR와_칸별_오류() throws Exception {
		mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
			.andExpect(jsonPath("$.data[0].field").value("purpose"))
			.andExpect(jsonPath("$.data[0].reason").value("필수 값입니다."));
	}

	@Test
	void B1_01_3_JSON_형식이_틀리면_400_VALIDATION_ERROR() throws Exception {
		mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@RestController
	static class SampleController {

		@PostMapping("/sample")
		ApiResponse<Void> create(@Valid @RequestBody SampleRequest request) {
			return ApiResponse.ok(null);
		}
	}

	record SampleRequest(@NotBlank(message = "필수 값입니다.") @Size(max = 200) String purpose) {
	}
}
