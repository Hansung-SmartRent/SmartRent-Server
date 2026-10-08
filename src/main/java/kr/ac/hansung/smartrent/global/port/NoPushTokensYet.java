package kr.ac.hansung.smartrent.global.port;

import org.springframework.stereotype.Component;

/** B2-13(푸시 토큰 표) 전까지 쓰는 기본 구현. B2-13에서 지웁니다 */
@Component
public class NoPushTokensYet implements PushTokenRemover {

	@Override
	public void remove(Long userId, String pushToken) {
	}
}
