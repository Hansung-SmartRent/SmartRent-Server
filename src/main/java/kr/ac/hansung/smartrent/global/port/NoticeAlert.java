package kr.ac.hansung.smartrent.global.port;

/**
 * 새 공지를 학생 전체에게 알리기(알림 종류 NOTICE, DB 설계 8절, openapi.yaml POST /admin/notices).
 * notifications 표는 백엔드 2의 B2-13에서 만들므로, 그 전까지는 서버 로그에만 남기는 기본 구현(NoNotificationsYet)이 쓰입니다.
 * 받을 학생과 푸시 여부는 B2-13이 명세대로 정하고, 이 인터페이스를 구현한 뒤 NoNotificationsYet의 이 부분을 지웁니다.
 */
public interface NoticeAlert {

	void toStudents(Long noticeId, String title, String body);
}
