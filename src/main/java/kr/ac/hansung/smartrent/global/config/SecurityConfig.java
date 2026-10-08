package kr.ac.hansung.smartrent.global.config;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.hansung.smartrent.global.security.JsonErrorWriter;
import kr.ac.hansung.smartrent.global.security.PublicPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 권한(API 명세 1-5절): 없음 = PublicPaths, 관리자 = /api/v1/admin/**, 나머지는 로그인한 사용자.
 * 학생 전용 동작은 컨트롤러에 @PreAuthorize("hasRole('STUDENT')"), 예약·연장처럼 승인·정지를 보는 동작은 @RequireApprovedStudent.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final RequestMatcher PUBLIC = new OrRequestMatcher(
		PublicPaths.PATHS.stream().map(p -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(p)).toList());

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonErrorWriter errors,
		JwtAuthenticationConverter converter) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC).permitAll()
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated())
			.oauth2ResourceServer(rs -> rs
				.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
				.bearerTokenResolver(bearerTokenResolver())
				.authenticationEntryPoint(errors)
				.accessDeniedHandler(errors))
			.exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
		return http.build();
	}

	/** 로그인 없이 부르는 주소에서는 Authorization 헤더를 읽지 않음(만료된 토큰이 붙어 와도 토큰 새로 받기가 되도록) */
	private static BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
		return (HttpServletRequest request) -> PUBLIC.matches(request) ? null : delegate.resolve(request);
	}

	/** 비밀번호는 BCrypt로만 저장합니다(DB 설계 3절 users.password_hash) */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
