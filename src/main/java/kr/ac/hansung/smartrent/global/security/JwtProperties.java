package kr.ac.hansung.smartrent.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml smartrent.jwt (.env의 JWT_SECRET, JWT_ACCESS_MINUTES, JWT_REFRESH_DAYS) */
@ConfigurationProperties(prefix = "smartrent.jwt")
public record JwtProperties(String secret, long accessMinutes, long refreshDays) {
}
