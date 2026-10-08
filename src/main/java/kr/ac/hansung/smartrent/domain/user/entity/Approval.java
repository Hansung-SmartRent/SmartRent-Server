package kr.ac.hansung.smartrent.domain.user.entity;

/** 학생증 승인 상태. NONE은 학생증 미제출 (DB 설계 3절) */
public enum Approval {
	NONE, PENDING, APPROVED, REJECTED
}
