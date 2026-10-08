package kr.ac.hansung.smartrent.domain.equipment.entity;

/** 기기 상태: 정상 / 점검 중 / 파손 (DB 설계 3절) */
public enum UnitCondition {
	NORMAL, INSPECTION, DAMAGED
}
