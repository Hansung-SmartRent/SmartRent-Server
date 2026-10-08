package kr.ac.hansung.smartrent.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 사례 AUTH-01: 소문자로 바꾼 뒤 @ 뒤 전체가 허용 도메인과 정확히 같아야 함 */
class EmailPolicyTest {

	@Test
	void AUTH_01_허용_4개와_거절_3개() {
		assertThat(EmailPolicy.isSchoolEmail("demo@hansung.ac.kr")).isTrue();
		assertThat(EmailPolicy.isSchoolEmail("DEMO@Hansung.AC.KR")).isTrue();
		assertThat(EmailPolicy.isSchoolEmail("a@hansung.kr")).isTrue();
		assertThat(EmailPolicy.isSchoolEmail("a@hansung.edu")).isTrue();
		assertThat(EmailPolicy.isSchoolEmail("a@myhansung.ac.kr")).isFalse();
		assertThat(EmailPolicy.isSchoolEmail("a@hansung.ac.kr.example.com")).isFalse();
		assertThat(EmailPolicy.isSchoolEmail("a@gmail.com")).isFalse();
	}
}
