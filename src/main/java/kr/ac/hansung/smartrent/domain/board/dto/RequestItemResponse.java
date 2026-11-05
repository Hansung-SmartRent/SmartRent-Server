package kr.ac.hansung.smartrent.domain.board.dto;

import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.board.entity.BoardRequest;

/** openapi.yaml RequestItem: 문의·구매 요청 한 건. userName은 쓴 학생 이름(탈퇴 등으로 없으면 null) */
public record RequestItemResponse(Long id, String userName, String title, String body, String response,
	LocalDateTime respondedAt, LocalDateTime createdAt) {

	public static RequestItemResponse of(BoardRequest r, String userName) {
		return new RequestItemResponse(r.getId(), userName, r.getTitle(), r.getBody(), r.getResponse(), r.getRespondedAt(),
			r.getCreatedAt());
	}
}
