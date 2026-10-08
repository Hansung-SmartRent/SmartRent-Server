package kr.ac.hansung.smartrent.global.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml smartrent.mail (.env의 MAIL_ENABLED) */
@ConfigurationProperties(prefix = "smartrent.mail")
public record MailProperties(boolean enabled) {
}
