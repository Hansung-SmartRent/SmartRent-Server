package kr.ac.hansung.smartrent.global.port;

/**
 * 로그아웃할 때 그 휴대폰의 푸시 토큰 지우기. push_tokens 표는 백엔드 2의 B2-13에서 만들므로,
 * 그 전까지는 아무것도 하지 않는 기본 구현(NoPushTokensYet)이 쓰입니다. B2-13에서 구현하고 NoPushTokensYet을 지웁니다.
 */
public interface PushTokenRemover {

	void remove(Long userId, String pushToken);
}
