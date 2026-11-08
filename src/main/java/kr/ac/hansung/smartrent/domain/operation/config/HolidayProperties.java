package kr.ac.hansung.smartrent.domain.operation.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * application.yml smartrent.holiday (.env의 HOLIDAY_SYNC_ENABLED, HOLIDAY_API_KEY, 외부 연결 4절).
 * 켜져 있는데 키가 비면 서버를 켜지 않고 빠진 이름만 알려 줍니다(값은 출력하지 않음). 꺼져 있으면 처음 데이터(fixtures)의 공휴일만 씁니다.
 */
@ConfigurationProperties(prefix = "smartrent.holiday")
public record HolidayProperties(boolean syncEnabled, String apiKey, String baseUrl, Duration timeout) {

	public HolidayProperties {
		if (syncEnabled && !StringUtils.hasText(apiKey)) {
			throw new IllegalStateException("HOLIDAY_SYNC_ENABLED=true이면 다음 설정이 필요합니다: HOLIDAY_API_KEY (docs/외부연결.md 4절)");
		}
		if (!StringUtils.hasText(baseUrl)) {
			baseUrl = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService";
		}
		if (timeout == null) {
			timeout = Duration.ofSeconds(10);
		}
	}

	/** 키가 로그·예외 메시지에 찍히지 않게 기록에서 숨깁니다 */
	@Override
	public String toString() {
		return "HolidayProperties[syncEnabled=" + syncEnabled + ", baseUrl=" + baseUrl + "]";
	}
}
