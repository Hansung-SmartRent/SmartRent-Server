package kr.ac.hansung.smartrent.global.port;

import org.springframework.stereotype.Component;

/** B2-12(경고 표) 전까지 쓰는 기본 구현. B2-12에서 지웁니다 */
@Component
public class NoWarningsYet implements ActiveWarningCounter {

	@Override
	public long count(Long userId) {
		return 0;
	}
}
