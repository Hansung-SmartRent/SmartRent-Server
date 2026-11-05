package kr.ac.hansung.smartrent.domain.recommendation.client;

import java.util.List;
import java.util.Map;

import kr.ac.hansung.smartrent.domain.recommendation.config.AiProperties;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gemini API REST 호출(외부 연결 3절). 키는 머리글 x-goog-api-key로만 보내고 로그에 남기지 않습니다.
 * 시간 초과(기본 20초)·키 오류·한도 초과(429)·서버 오류는 모두 AI_UNAVAILABLE이고 자동으로 다시 부르지 않습니다.
 */
@Slf4j
public class GeminiClient implements AiClient {

	private final RestClient restClient;
	private final AiProperties properties;
	private final JsonMapper jsonMapper;

	public GeminiClient(AiProperties properties, JsonMapper jsonMapper) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		var http = java.net.http.HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
		var factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(properties.timeout());
		this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
	}

	@Override
	public String generate(String prompt) {
		Map<String, Object> body = Map.of(
			"contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
			"generationConfig", Map.of("responseMimeType", "application/json"));
		try {
			String raw = restClient.post()
				.uri("/v1beta/models/{model}:generateContent", properties.modelId())
				.header("x-goog-api-key", properties.apiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(String.class);
			JsonNode text = jsonMapper.readTree(raw).path("candidates").path(0).path("content").path("parts").path(0).path("text");
			if (!text.isString()) {
				throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
			}
			return text.asString();
		}
		catch (RestClientException | tools.jackson.core.JacksonException e) {
			log.warn("Gemini 호출 실패: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
		}
	}
}
