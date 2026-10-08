package kr.ac.hansung.smartrent.global.mail;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

/** B1-05 완료 조건: MAIL_ENABLED=false일 때 로그 대체는 local에서만, prod는 MAIL_UNAVAILABLE */
class MailConfigTest {

	private CodeMailer mailer(String profile) {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles(profile);
		return new MailConfig().codeMailer(new MailProperties(false), env,
			new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class));
	}

	@Test
	void B1_05_local은_메일_대신_로그로_대체() {
		assertThatCode(() -> mailer("local").send("a@hansung.ac.kr", "123456")).doesNotThrowAnyException();
	}

	@Test
	void B1_05_prod는_로그_대체가_켜지지_않고_MAIL_UNAVAILABLE() {
		assertThatThrownBy(() -> mailer("prod").send("a@hansung.ac.kr", "123456"))
			.isInstanceOf(BusinessException.class)
			.hasMessage("메일을 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요.");
	}
}
