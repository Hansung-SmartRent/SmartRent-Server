package kr.ac.hansung.smartrent.domain.board.entity;

import java.time.LocalDateTime;

/** 문의와 구매 요청의 공통 모양(두 표의 칸이 같음, DB 설계 3절). 서비스 하나로 두 종류를 함께 다룹니다 */
public interface BoardRequest {

	Long getId();

	Long getUserId();

	String getTitle();

	String getBody();

	String getResponse();

	LocalDateTime getRespondedAt();

	LocalDateTime getCreatedAt();

	/** 관리자 답변과 답변 시각 */
	void answer(String response, LocalDateTime respondedAt);
}
