package kr.ac.hansung.smartrent.domain.board.dto;

import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.board.entity.Notice;

/** openapi.yaml Notice */
public record NoticeResponse(Long id, String title, String body, LocalDateTime createdAt, LocalDateTime updatedAt) {

	public static NoticeResponse of(Notice n) {
		return new NoticeResponse(n.getId(), n.getTitle(), n.getBody(), n.getCreatedAt(), n.getUpdatedAt());
	}
}
