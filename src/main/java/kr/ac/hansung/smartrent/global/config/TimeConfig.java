package kr.ac.hansung.smartrent.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 지금 시각은 이 Clock을 주입받아 LocalDateTime.now(clock)으로 얻습니다.
 * 시험에서는 Clock.fixed(...)로 바꿔 넣습니다(CONVENTION 2절).
 */
@Configuration
public class TimeConfig {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.system(ZONE);
	}
}
