package kr.ac.hansung.smartrent.global.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.global.config.TimeConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/** B1-02 범위: 공용 관리 기록 서비스가 Clock 기준 시각으로 기록을 남긴다 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, AuditLogServiceTest.FixedClock.class})
class AuditLogServiceTest {

	static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 10, 0);

	@TestConfiguration
	static class FixedClock {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant(), TimeConfig.ZONE);
		}
	}

	@Autowired
	AuditLogService service;

	@Autowired
	AuditLogRepository repository;

	@AfterEach
	void clean() {
		repository.deleteAll();
	}

	@Test
	void B1_02_관리_기록이_고정한_Clock_시각으로_저장된다() {
		AuditLog saved = service.record("PICKUP", "대여 120 / 기기 278", null);

		AuditLog found = repository.findById(saved.getId()).orElseThrow();
		assertThat(found.getAction()).isEqualTo("PICKUP");
		assertThat(found.getDetail()).isEqualTo("대여 120 / 기기 278");
		assertThat(found.getActorId()).isNull();
		assertThat(found.getCreatedAt()).isEqualTo(NOW);
	}
}
