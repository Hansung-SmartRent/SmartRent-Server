package kr.ac.hansung.smartrent.global.security;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** 지금 요청한 사용자의 id(액세스 토큰의 sub). 컨트롤러·서비스에서 CurrentUser.id()로 씁니다 */
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Long id() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return Long.valueOf(jwt.getSubject());
	}
}
