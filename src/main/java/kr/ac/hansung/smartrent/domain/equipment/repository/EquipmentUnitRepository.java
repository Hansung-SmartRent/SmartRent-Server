package kr.ac.hansung.smartrent.domain.equipment.repository;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentUnitRepository extends JpaRepository<EquipmentUnit, Long> {

	/** 지워지지 않은 기기 */
	List<EquipmentUnit> findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(Long modelId);
}
