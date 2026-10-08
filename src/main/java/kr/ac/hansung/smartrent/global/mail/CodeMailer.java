package kr.ac.hansung.smartrent.global.mail;

/** 인증번호 메일 보내기. 실패하면 BusinessException(MAIL_UNAVAILABLE) */
public interface CodeMailer {

	void send(String email, String code);
}
