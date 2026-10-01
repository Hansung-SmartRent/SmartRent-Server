package kr.ac.hansung.smartrent.global.config;

import java.util.List;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * 서버를 켤 때 필수 설정이 비어 있으면 어느 이름이 빠졌는지 알려 주고 멈춥니다.
 * 값은 출력하지 않습니다. 새 필수값이 생기면 REQUIRED에 이름을 더합니다.
 * 시험은 main을 거치지 않으므로 이 검사를 하지 않습니다(DB는 Testcontainers).
 */
public class RequiredSettingsCheck implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	static final List<String> REQUIRED = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD");

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		check(event.getEnvironment());
	}

	static void check(Environment environment) {
		List<String> missing = REQUIRED.stream()
			.filter(name -> !StringUtils.hasText(environment.getProperty(name)))
			.toList();
		if (!missing.isEmpty()) {
			throw new IllegalStateException("필수 설정이 비어 있습니다: " + String.join(", ", missing)
				+ ". 프로젝트 루트의 .env 또는 OS 환경변수에 넣어 주세요(docs/시작하기.md 5절).");
		}
	}
}
