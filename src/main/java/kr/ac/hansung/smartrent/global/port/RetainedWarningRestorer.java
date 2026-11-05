package kr.ac.hansung.smartrent.global.port;

/**
 * 탈퇴 뒤 같은 학번으로 다시 가입한 학생을 승인할 때, 보관 중인 경고·정지 기록을 되살리기(DB 설계 3절 retained_warnings).
 * 백엔드 2의 B2-17에서 만들므로, 그 전까지는 아무것도 하지 않는 기본 구현(NoWarningsYet)이 쓰입니다.
 * B2-17에서 이 인터페이스를 구현하고 NoWarningsYet의 이 부분을 지웁니다. 승인과 같은 트랜잭션에서 불립니다.
 */
public interface RetainedWarningRestorer {

	void restore(Long userId, String studentNumber);
}
