package kr.ac.hansung.smartrent.global.mail;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 인증 메일 보내는 방법 고르기(외부 연결 1절)
 * - MAIL_ENABLED=true: Gmail SMTP로 보냄
 * - false + local 프로필: 보내지 않고 서버 로그에 번호를 찍음(개발용)
 * - false + 그 밖(prod): 로그에 찍지 않고 MAIL_UNAVAILABLE
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

	@Bean
	public CodeMailer codeMailer(MailProperties properties, Environment environment,
		ObjectProvider<JavaMailSender> mailSender) {
		if (properties.enabled()) {
			JavaMailSender sender = mailSender.getObject();
			String from = environment.getProperty("spring.mail.username");
			return (email, code) -> {
				SimpleMailMessage message = new SimpleMailMessage();
				message.setFrom(from);
				message.setTo(email);
				message.setSubject("[SmartRent] 인증번호");
				message.setText("SmartRent 인증번호는 " + code + " 입니다.\n5분 안에 입력해 주세요.");
				try {
					sender.send(message);
				}
				catch (MailException e) {
					log.warn("인증 메일을 보내지 못했습니다: {}", e.getClass().getSimpleName());
					throw new BusinessException(ErrorCode.MAIL_UNAVAILABLE);
				}
			};
		}
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			return (email, code) -> log.info("[개발용, 메일 대신 로그] {} 인증번호: {}", email, code);
		}
		log.warn("MAIL_ENABLED=false라 인증 메일을 보낼 수 없습니다. 인증번호 요청은 MAIL_UNAVAILABLE로 답합니다.");
		return (email, code) -> {
			throw new BusinessException(ErrorCode.MAIL_UNAVAILABLE);
		};
	}
}
