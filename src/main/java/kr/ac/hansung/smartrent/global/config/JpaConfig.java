package kr.ac.hansung.smartrent.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** created_at·updated_at을 Clock 기준 한국 시각으로 넣습니다. 시험에서 Clock을 고정하면 이 시각도 고정됩니다. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "clockDateTimeProvider")
public class JpaConfig {

	@Bean
	public DateTimeProvider clockDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}
}
