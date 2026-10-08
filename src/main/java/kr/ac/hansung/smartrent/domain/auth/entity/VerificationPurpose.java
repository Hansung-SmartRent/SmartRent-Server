package kr.ac.hansung.smartrent.domain.auth.entity;

/** 인증번호를 보낸 목적 (DB 설계 3절) */
public enum VerificationPurpose {
	SIGNUP, PASSWORD_RESET, EMAIL_CHANGE
}
