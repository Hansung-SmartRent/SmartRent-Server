package kr.ac.hansung.smartrent.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** B1-01 서버 구현 후 확인할 것 9행: 필수값이 빠지면 이름을 알리고 값은 출력하지 않음 */
class RequiredSettingsCheckTest {

	@Test
	void B1_01_9_필수값이_모두_있으면_통과() {
		MockEnvironment env = new MockEnvironment()
			.withProperty("DB_URL", "jdbc:mysql://localhost:3306/smartrent")
			.withProperty("DB_USERNAME", "smartrent")
			.withProperty("DB_PASSWORD", "secret-value");

		assertThatCode(() -> RequiredSettingsCheck.check(env)).doesNotThrowAnyException();
	}

	@Test
	void B1_01_9_DB_PASSWORD가_비면_이름만_알리고_멈춘다() {
		MockEnvironment env = new MockEnvironment()
			.withProperty("DB_URL", "jdbc:mysql://localhost:3306/smartrent")
			.withProperty("DB_USERNAME", "smartrent-user")
			.withProperty("DB_PASSWORD", "");

		assertThatThrownBy(() -> RequiredSettingsCheck.check(env))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("DB_PASSWORD")
			.hasMessageNotContaining("DB_URL")
			.hasMessageNotContaining("smartrent-user");
	}
}
