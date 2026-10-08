package kr.ac.hansung.smartrent.support;

import java.util.LinkedHashMap;
import java.util.Map;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.mail.CodeMailer;

/** 시험용 메일: 보낸 번호를 기억하고, fail=true면 SMTP 실패처럼 MAIL_UNAVAILABLE */
public class FakeMailer implements CodeMailer {

	public final Map<String, String> lastCode = new LinkedHashMap<>();
	public boolean fail;

	@Override
	public void send(String email, String code) {
		if (fail) {
			throw new BusinessException(ErrorCode.MAIL_UNAVAILABLE);
		}
		lastCode.put(email, code);
	}
}
