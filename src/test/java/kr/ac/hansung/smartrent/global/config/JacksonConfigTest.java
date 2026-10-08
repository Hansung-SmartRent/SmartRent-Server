package kr.ac.hansung.smartrent.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** API 명세 1-2절: 시각은 +09:00을 붙여 초 단위로. 요청은 다른 시간대여도 한국 시각으로 바꿈 */
class JacksonConfigTest {

	JsonMapper mapper = JsonMapper.builder().addModule(new JacksonConfig().koreanDateTimeModule()).build();

	record Holder(LocalDateTime at) {
	}

	@Test
	void 응답_시각은_초_단위에_플러스09가_붙는다() {
		String json = mapper.writeValueAsString(new Holder(LocalDateTime.of(2026, 10, 7, 10, 0, 0, 123_456_789)));
		assertThat(json).isEqualTo("{\"at\":\"2026-10-07T10:00:00+09:00\"}");
	}

	@Test
	void 요청_시각은_시간대를_한국_시각으로_바꿔_읽는다() {
		assertThat(mapper.readValue("{\"at\":\"2026-10-07T10:00:00+09:00\"}", Holder.class).at())
			.isEqualTo(LocalDateTime.of(2026, 10, 7, 10, 0));
		assertThat(mapper.readValue("{\"at\":\"2026-10-07T01:00:00Z\"}", Holder.class).at())
			.isEqualTo(LocalDateTime.of(2026, 10, 7, 10, 0));
	}
}
