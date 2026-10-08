package kr.ac.hansung.smartrent.global.port;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** B2-13(알림 표) 전까지 쓰는 기본 구현. 로그에만 남김. B2-13에서 지웁니다 */
@Slf4j
@Component
public class NoNotificationsYet implements OperationsAlert {

	@Override
	public void send(String title, String body) {
		log.warn("[관리자 알림 OPERATIONS] {}: {}", title, body);
	}
}
