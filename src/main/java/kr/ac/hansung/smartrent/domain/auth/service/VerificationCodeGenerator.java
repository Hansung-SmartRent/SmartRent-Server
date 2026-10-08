package kr.ac.hansung.smartrent.domain.auth.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** 6자리 인증번호 만들기. 시험에서는 사례의 번호(예: 483920)를 돌려주는 것으로 바꿔 씁니다 */
@Component
public class VerificationCodeGenerator {

	private static final SecureRandom RANDOM = new SecureRandom();

	public String next() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}
}
