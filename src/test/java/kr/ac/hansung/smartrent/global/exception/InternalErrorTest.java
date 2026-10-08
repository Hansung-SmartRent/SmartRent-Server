package kr.ac.hansung.smartrent.global.exception;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 처리하지 못한 예외는 500 INTERNAL_ERROR(API 명세 1-6절) */
class InternalErrorTest {

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new BrokenController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void 예상하지_못한_예외는_500_INTERNAL_ERROR이고_원인은_응답에_없다() throws Exception {
		mockMvc.perform(get("/broken"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.data").isEmpty())
			.andExpect(content().string(not(containsString("unit is null"))));
	}

	@Test
	void 허용하지_않는_메서드는_500이_아니라_405() throws Exception {
		mockMvc.perform(delete("/broken"))
			.andExpect(status().isMethodNotAllowed());
	}

	@RestController
	static class BrokenController {

		@GetMapping("/broken")
		String broken() {
			throw new IllegalStateException("unit is null");
		}
	}
}
