package kr.ac.hansung.smartrent.global.port;

/**
 * 문의·구매 요청 알림(DB 설계 8절). 새 요청은 관리자에게 REQUEST, 답변은 학생에게 RESPONSE이고,
 * relatedType은 INQUIRY 또는 PURCHASE_REQUEST, relatedId는 그 기록 id(API 명세 3절).
 * notifications 표는 백엔드 2의 B2-13에서 만들므로, 그 전까지는 서버 로그에만 남기는 기본 구현(NoNotificationsYet)이 쓰입니다.
 */
public interface BoardAlert {

	void newRequest(String relatedType, Long relatedId, String title);

	void answered(Long userId, String relatedType, Long relatedId, String title);
}
