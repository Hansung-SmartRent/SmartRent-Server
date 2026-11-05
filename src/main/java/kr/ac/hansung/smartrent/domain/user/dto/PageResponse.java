package kr.ac.hansung.smartrent.domain.user.dto;

import java.util.List;

/** 관리자 목록의 쪽 나누기 응답(API 명세 1-4절): items와 page(쪽 번호·크기·전체 개수·전체 쪽 수) */
public record PageResponse<T>(List<T> items, Meta page) {

	public record Meta(long page, long size, long totalElements, long totalPages) {
	}

	/** 이미 거른 전체 목록을 쪽으로 자름. page는 0부터 */
	public static <T> PageResponse<T> of(List<T> all, int page, int size) {
		int from = (int) Math.min((long) page * size, all.size());
		int to = Math.min(from + size, all.size());
		long totalPages = all.isEmpty() ? 0 : (all.size() + size - 1) / size;
		return new PageResponse<>(List.copyOf(all.subList(from, to)), new Meta(page, size, all.size(), totalPages));
	}
}
