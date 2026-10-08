package kr.ac.hansung.smartrent.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** @RequireApprovedStudent가 붙은 컨트롤러 메서드를 부르기 전에 학생 상태를 확인합니다 */
@Configuration
@RequiredArgsConstructor
public class StudentAccessInterceptor implements HandlerInterceptor, WebMvcConfigurer {

	private final StudentAccessGuard guard;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(RequireApprovedStudent.class)) {
			guard.check(CurrentUser.id());
		}
		return true;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this);
	}
}
