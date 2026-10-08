package kr.ac.hansung.smartrent.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger 화면: /swagger-ui/index.html */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI().info(new Info()
			.title("SmartRent API")
			.description("한성대 기자재 대여 서비스 서버 API. 규칙 설명은 docs/api/README.md")
			.version("v1"));
	}
}
