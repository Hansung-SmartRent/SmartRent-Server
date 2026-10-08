package kr.ac.hansung.smartrent.global.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml smartrent.seed (.env의 SEED_PROD_ENABLED, ADMIN_EMAIL, ADMIN_PASSWORD) */
@ConfigurationProperties(prefix = "smartrent.seed")
public record SeedProperties(boolean prodEnabled, String adminEmail, String adminPassword) {
}
