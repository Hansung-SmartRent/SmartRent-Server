package kr.ac.hansung.smartrent.global.port;

/**
 * 관리자에게 운영 문제 알리기(알림 종류 OPERATIONS, DB 설계 8절). notifications 표는 백엔드 2의 B2-13에서 만들므로,
 * 그 전까지는 서버 로그에만 남기는 기본 구현(NoNotificationsYet)이 쓰입니다. B2-13에서 구현하고 NoNotificationsYet을 지웁니다.
 */
public interface OperationsAlert {

	void send(String title, String body);
}
