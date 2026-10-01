package kr.ac.hansung.smartrent;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 시험용 MySQL. 개인 DB 대신 시험이 끝나면 지워지는 컨테이너에 연결합니다.
 * DB가 필요한 @SpringBootTest에 @Import(TestcontainersConfiguration.class)로 붙입니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer("mysql:9.7").withEnv("TZ", "Asia/Seoul");
	}
}
