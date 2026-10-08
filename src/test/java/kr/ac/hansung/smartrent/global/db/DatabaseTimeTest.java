package kr.ac.hansung.smartrent.global.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** B1-02 서버 구현 후 확인할 것 5행: DB의 NOW()와 서버 Clock이 둘 다 한국 시각 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseTimeTest {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Clock clock;

	@Test
	void B1_02_5_DB의_NOW와_서버의_지금이_같은_한국_시각() {
		LocalDateTime db = jdbc.queryForObject("SELECT NOW()", LocalDateTime.class);
		LocalDateTime server = LocalDateTime.now(clock);

		assertThat(Duration.between(db, server).abs()).isLessThan(Duration.ofSeconds(5));
		assertThat(jdbc.queryForObject("SELECT TIMESTAMPDIFF(HOUR, UTC_TIMESTAMP(), NOW())", Integer.class)).isEqualTo(9);
	}
}
