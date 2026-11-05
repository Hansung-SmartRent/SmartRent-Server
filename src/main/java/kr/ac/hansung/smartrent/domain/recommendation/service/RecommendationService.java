package kr.ac.hansung.smartrent.domain.recommendation.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.recommendation.client.AiClient;
import kr.ac.hansung.smartrent.domain.recommendation.config.AiProperties;
import kr.ac.hansung.smartrent.domain.recommendation.dto.RecommendationResponse;
import kr.ac.hansung.smartrent.domain.recommendation.entity.RecommendationLog;
import kr.ac.hansung.smartrent.domain.recommendation.repository.RecommendationLogRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 기자재 추천(B1-21, 외부 연결 3절). 요청에는 운영 중인 모델 목록과 사용 목적만 넣고 계정 정보는 넣지 않습니다.
 * AI가 돌려준 modelId 중 우리 목록에 있는 것만 돌려주고, 요청·결과를 recommendation_logs에 남깁니다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

	/** 대체 추천일 때 recommendation_logs.model_id_used에 남기는 표시 */
	public static final String FALLBACK = "fallback";
	static final int FALLBACK_LIMIT = 3;

	private final AiClient aiClient;
	private final AiProperties properties;
	private final EquipmentModelRepository modelRepository;
	private final RecommendationLogRepository logRepository;
	private final JsonMapper jsonMapper;

	/** AI 응답을 기다리는 동안 DB 연결을 잡지 않도록 트랜잭션으로 묶지 않음. 기록 저장은 저장소가 따로 처리 */
	public RecommendationResponse recommend(Long userId, String purpose) {
		String trimmed = purpose.trim();
		List<EquipmentModel> models = modelRepository.findAllByArchivedFalseOrderByIdAsc();
		List<RecommendationResponse.Item> items;
		String modelIdUsed;
		if (properties.enabled()) {
			items = parse(aiClient.generate(prompt(models, trimmed)), models);
			modelIdUsed = properties.modelId();
		}
		else {
			items = fallback(models, trimmed);
			modelIdUsed = FALLBACK;
		}
		RecommendationResponse response = new RecommendationResponse(items, properties.enabled() && properties.modelVerified());
		logRepository.save(RecommendationLog.builder()
			.userId(userId)
			.purpose(trimmed)
			.resultJson(jsonMapper.writeValueAsString(response))
			.modelIdUsed(modelIdUsed)
			.build());
		return response;
	}

	/** 제공자에게 보내는 지시문. 모델 목록(id, 이름, 카테고리, 활용 방안, 장기 여부)과 사용 목적만 들어감 */
	String prompt(List<EquipmentModel> models, String purpose) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (EquipmentModel m : models) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("modelId", m.getId());
			row.put("name", m.getName());
			row.put("category", m.getCategory());
			row.put("purpose", m.getPurpose());
			row.put("longTerm", m.getRentalType() == RentalType.LONG_TERM);
			list.add(row);
		}
		return """
			너는 대학교 기자재실의 기자재 추천 도우미야. 아래 "기자재 목록"에 있는 모델 중에서만 학생의 사용 목적에 맞는 것을 최대 3개 골라.
			목록에 없는 기자재는 말하지 마. 대여 가능 여부는 판단하지 마.
			반드시 다음 JSON 하나로만 답해: {"items":[{"modelId":숫자,"reason":"한국어 한두 문장"}]}
			맞는 기자재가 없으면 {"items":[]}로 답해.

			기자재 목록:
			%s

			학생의 사용 목적:
			%s
			""".formatted(jsonMapper.writeValueAsString(list), purpose);
	}

	/** AI 답(JSON)을 읽어 우리 목록에 있는 modelId만 남김. JSON이 아니면 AI_UNAVAILABLE */
	List<RecommendationResponse.Item> parse(String text, List<EquipmentModel> models) {
		Map<Long, EquipmentModel> byId = models.stream().collect(Collectors.toMap(EquipmentModel::getId, Function.identity()));
		JsonNode items;
		try {
			items = jsonMapper.readTree(stripFence(text)).path("items");
		}
		catch (JacksonException e) {
			throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
		}
		if (!items.isArray()) {
			throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
		}
		List<RecommendationResponse.Item> result = new ArrayList<>();
		for (JsonNode item : items) {
			EquipmentModel model = item.path("modelId").canConvertToLong() ? byId.get(item.path("modelId").asLong()) : null;
			if (model != null && result.stream().noneMatch(r -> r.modelId().equals(model.getId()))) {
				result.add(new RecommendationResponse.Item(model.getId(), model.getName(), item.path("reason").asString("")));
			}
		}
		return result;
	}

	/** 개발용 대체 추천: 모델 이름에 사용 목적의 단어가 들어간 모델 3개까지 */
	List<RecommendationResponse.Item> fallback(List<EquipmentModel> models, String purpose) {
		List<String> words = List.of(purpose.toLowerCase(Locale.ROOT).split("\\s+")).stream().filter(w -> !w.isBlank()).toList();
		return models.stream()
			.filter(m -> words.stream().anyMatch(w -> m.getName().toLowerCase(Locale.ROOT).contains(w)))
			.limit(FALLBACK_LIMIT)
			.map(m -> new RecommendationResponse.Item(m.getId(), m.getName(), "모델 이름에 사용 목적의 단어가 들어 있습니다(개발용 대체 추천)."))
			.toList();
	}

	/** 답을 ```json ... ``` 으로 감싸 오는 경우만 벗김 */
	private static String stripFence(String text) {
		String t = text == null ? "" : text.trim();
		if (t.startsWith("```")) {
			t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
		}
		return t;
	}
}
