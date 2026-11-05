package kr.ac.hansung.smartrent.global.port;

import java.util.List;

import org.springframework.stereotype.Component;

/** B2-12(경고)·B2-04(대여 조회) 전까지 쓰는 기본 구현. 빈 목록. 백엔드 2가 구현하면 지웁니다 */
@Component
public class NoHistoryYet implements StudentHistoryReader {

	@Override
	public List<?> warnings(Long userId) {
		return List.of();
	}

	@Override
	public List<?> rentals(Long userId) {
		return List.of();
	}
}
