package kr.ac.hansung.smartrent.domain.equipment.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;

/** 기기 이름·상태 변경 요청(openapi UnitPatch). 보낸 칸만 바꿈 */
public record UnitPatchRequest(@Size(max = 100) @Pattern(regexp = ".*\\S.*") String name, UnitCondition condition) {
}
