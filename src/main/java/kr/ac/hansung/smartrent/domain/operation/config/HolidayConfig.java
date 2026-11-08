package kr.ac.hansung.smartrent.domain.operation.config;

import kr.ac.hansung.smartrent.domain.operation.client.DataGoKrHolidayClient;
import kr.ac.hansung.smartrent.domain.operation.client.HolidayClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(HolidayProperties.class)
public class HolidayConfig {

	@Bean
	HolidayClient holidayClient(HolidayProperties properties, JsonMapper jsonMapper) {
		return new DataGoKrHolidayClient(properties, jsonMapper);
	}
}
