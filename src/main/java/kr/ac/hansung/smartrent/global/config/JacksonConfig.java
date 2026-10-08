package kr.ac.hansung.smartrent.global.config;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * API의 시각은 한국 시각에 +09:00을 붙여 초 단위로 주고받습니다(API 명세 1-2절, DB 설계 1절). 예: 2026-10-07T10:00:00+09:00
 * 서버 안에서는 LocalDateTime(한국 시각)으로 다룹니다. 요청에 다른 시간대가 붙어 오면 한국 시각으로 바꿉니다.
 */
@Configuration
public class JacksonConfig {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	@Bean
	public SimpleModule koreanDateTimeModule() {
		SimpleModule module = new SimpleModule("korean-date-time");
		module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
			@Override
			public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
				gen.writeString(value.truncatedTo(ChronoUnit.SECONDS).atZone(TimeConfig.ZONE).format(FORMAT));
			}
		});
		module.addDeserializer(LocalDateTime.class, new ValueDeserializer<>() {
			@Override
			public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
				String text = p.getString().trim();
				if (text.length() > 19 && (text.endsWith("Z") || text.charAt(text.length() - 6) == '+' || text.charAt(text.length() - 6) == '-')) {
					return OffsetDateTime.parse(text).atZoneSameInstant(TimeConfig.ZONE).toLocalDateTime();
				}
				return LocalDateTime.parse(text);
			}
		});
		return module;
	}
}
