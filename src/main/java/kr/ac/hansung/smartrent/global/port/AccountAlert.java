package kr.ac.hansung.smartrent.global.port;

/**
 * 계정 알림 보내기(알림 종류 ACCOUNT, DB 설계 8절). 예: 학생증 제출(관리자에게), 승인·반려(학생에게).
 * notifications 표는 백엔드 2의 B2-13에서 만들므로, 그 전까지는 서버 로그에만 남기는 기본 구현(NoNotificationsYet)이 쓰입니다.
 * B2-13에서 구현하고 NoNotificationsYet의 이 부분을 지웁니다.
 */
public interface AccountAlert {

	/** relatedUserId: 알림을 누르면 열 학생 */
	void toAdmins(String title, String body, Long relatedUserId);

	/** 학생 한 명에게(승인·반려 결과) */
	void toStudent(Long userId, String title, String body);
}
