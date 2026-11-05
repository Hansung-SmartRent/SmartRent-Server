package kr.ac.hansung.smartrent.domain.recommendation.client;

/** 추천 문장을 만드는 AI 제공자. 실패(시간 초과, 키 오류, 한도 초과, 서버 오류)는 BusinessException(AI_UNAVAILABLE) */
public interface AiClient {

	/** 지시문을 보내고 제공자가 돌려준 글(JSON 문자열이어야 함)을 그대로 돌려줍니다. 다시 부르지 않습니다 */
	String generate(String prompt);
}
