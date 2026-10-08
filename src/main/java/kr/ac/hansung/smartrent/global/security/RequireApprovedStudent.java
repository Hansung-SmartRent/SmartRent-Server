package kr.ac.hansung.smartrent.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 승인된 학생만, 정지 중이 아닐 때만 부를 수 있는 동작(예약하기, 연장 등)에 붙입니다.
 * 승인 전·반려·미제출이면 403 NOT_APPROVED, 정지 중이면 403 SUSPENDED (API 명세 1-5절, 기획안 1·7장).
 * 관리자가 학생 대신 처리하는 현장 대여는 서비스에서 StudentAccessGuard.check(학생)을 직접 부릅니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireApprovedStudent {
}
