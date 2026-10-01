package kr.ac.hansung.smartrent.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.TimeZone;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** B1-01 서버 구현 후 확인할 것 4행과 시간대 설정 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TimeConfigTest {

	@Autowired
	Clock clock;

	@Test
	void B1_01_4_고정한_Clock으로_지금_시각을_바꿀_수_있다() {
		Clock fixed = Clock.fixed(OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant(), TimeConfig.ZONE);

		assertThat(LocalDateTime.now(fixed)).isEqualTo(LocalDateTime.of(2026, 10, 7, 10, 0));
	}

	@Test
	void B1_01_4_서버_Clock과_JVM_기본_시간대는_한국_시각() {
		assertThat(clock.getZone()).isEqualTo(TimeConfig.ZONE);
		assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
	}
}
