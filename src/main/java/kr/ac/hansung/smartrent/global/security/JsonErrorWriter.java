package kr.ac.hansung.smartrent.global.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** 보안 단계에서 막힌 요청도 공통 응답 틀로 답함: 토큰 없음·만료·위조 401 UNAUTHORIZED, 권한 없음 403 FORBIDDEN */
@Component
@RequiredArgsConstructor
public class JsonErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	@Override
	public void commence(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
		AuthenticationException e) throws IOException {
		write(response, ErrorCode.UNAUTHORIZED);
	}

	@Override
	public void handle(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException e) throws IOException {
		write(response, ErrorCode.FORBIDDEN);
	}

	private void write(HttpServletResponse response, ErrorCode code) throws IOException {
		response.setStatus(code.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), ApiResponse.fail(code));
	}
}
