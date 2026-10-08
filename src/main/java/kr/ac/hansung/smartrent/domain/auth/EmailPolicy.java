package kr.ac.hansung.smartrent.domain.auth;

import java.util.Locale;
import java.util.Set;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;

/** 학교 이메일만 허용: 소문자로 바꾼 뒤 @ 뒤 전체가 세 도메인 중 하나와 정확히 같아야 함(기획안 1장, 사례 AUTH-01) */
public final class EmailPolicy {

	public static final Set<String> ALLOWED_DOMAINS = Set.of("hansung.ac.kr", "hansung.kr", "hansung.edu");

	private EmailPolicy() {
	}

	public static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public static boolean isSchoolEmail(String email) {
		String e = normalize(email);
		int at = e.lastIndexOf('@');
		return at > 0 && ALLOWED_DOMAINS.contains(e.substring(at + 1));
	}

	/** 학교 이메일이면 소문자로 바꿔 돌려주고, 아니면 EMAIL_DOMAIN_NOT_ALLOWED */
	public static String requireSchoolEmail(String email) {
		if (!isSchoolEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_DOMAIN_NOT_ALLOWED);
		}
		return normalize(email);
	}
}
