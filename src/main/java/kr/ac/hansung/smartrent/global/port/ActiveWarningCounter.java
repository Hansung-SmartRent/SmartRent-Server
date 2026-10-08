package kr.ac.hansung.smartrent.global.port;

/**
 * 학생의 유효 경고 수. 경고 표는 백엔드 2의 B2-12에서 만들므로, 그 전까지는 0을 돌려주는 기본 구현(NoWarningsYet)이 쓰입니다.
 * B2-12에서 이 인터페이스를 구현한 빈을 만들고 NoWarningsYet을 지웁니다.
 */
public interface ActiveWarningCounter {

	long count(Long userId);
}
