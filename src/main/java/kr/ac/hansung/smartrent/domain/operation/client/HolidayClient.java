package kr.ac.hansung.smartrent.domain.operation.client;

import java.time.LocalDate;
import java.util.List;

/** 공휴일 받기(외부 연결 4절). 실패하면 예외를 던지고, 받은 쪽은 기존 달력을 그대로 둡니다 */
public interface HolidayClient {

	List<PublicHoliday> fetch(int year);

	record PublicHoliday(LocalDate date, String name) {
	}
}
