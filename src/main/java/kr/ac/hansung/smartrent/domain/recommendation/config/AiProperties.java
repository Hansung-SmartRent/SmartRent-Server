package kr.ac.hansung.smartrent.domain.recommendation.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * application.yml smartrent.ai (.env의 AI_ENABLED, GEMINI_API_KEY, AI_MODEL_ID, AI_MODEL_VERIFIED).
 * 켜져 있는데 키나 모델 ID가 비면 서버를 켜지 않고 빠진 이름만 알려 줍니다(값은 출력하지 않음).
 */
@ConfigurationProperties(prefix = "smartrent.ai")
public record AiProperties(boolean enabled, String apiKey, String modelId, boolean modelVerified, String baseUrl,
	Duration timeout) {

	public AiProperties {
		if (enabled && (!StringUtils.hasText(apiKey) || !StringUtils.hasText(modelId))) {
			String missing = !StringUtils.hasText(apiKey) && !StringUtils.hasText(modelId) ? "GEMINI_API_KEY, AI_MODEL_ID"
				: !StringUtils.hasText(apiKey) ? "GEMINI_API_KEY" : "AI_MODEL_ID";
			throw new IllegalStateException("AI_ENABLED=true이면 다음 설정이 필요합니다: " + missing + " (docs/외부연결.md 3절)");
		}
		if (timeout == null) {
			timeout = Duration.ofSeconds(20);
		}
	}

	/** 키가 로그·예외 메시지에 찍히지 않게 기록에서 숨깁니다 */
	@Override
	public String toString() {
		return "AiProperties[enabled=" + enabled + ", modelId=" + modelId + ", modelVerified=" + modelVerified + "]";
	}
}
