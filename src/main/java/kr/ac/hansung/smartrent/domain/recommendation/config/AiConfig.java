package kr.ac.hansung.smartrent.domain.recommendation.config;

import kr.ac.hansung.smartrent.domain.recommendation.client.AiClient;
import kr.ac.hansung.smartrent.domain.recommendation.client.GeminiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

	@Bean
	AiClient aiClient(AiProperties properties, JsonMapper jsonMapper) {
		return new GeminiClient(properties, jsonMapper);
	}
}
