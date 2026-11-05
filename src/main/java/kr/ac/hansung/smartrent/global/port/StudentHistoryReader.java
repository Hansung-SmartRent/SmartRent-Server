package kr.ac.hansung.smartrent.global.port;

import java.util.List;

/**
 * 관리자 학생 상세(B1-14)에 붙일 경고 목록과 대여 기록. 경고는 백엔드 2의 B2-12(openapi.yaml WarningItem),
 * 대여는 B2-04(openapi.yaml Rental)에서 만들므로, 그 전까지는 빈 목록을 돌려주는 기본 구현(NoHistoryYet)이 쓰입니다.
 * 백엔드 2가 이 인터페이스를 구현한 빈을 만들고 NoHistoryYet을 지웁니다. 돌려준 객체는 응답에 그대로 들어갑니다.
 */
public interface StudentHistoryReader {

	/** 그 학생의 경고(유효·취소·만료 모두, 최신순) */
	List<?> warnings(Long userId);

	/** 그 학생의 대여 기록(최신순) */
	List<?> rentals(Long userId);
}
