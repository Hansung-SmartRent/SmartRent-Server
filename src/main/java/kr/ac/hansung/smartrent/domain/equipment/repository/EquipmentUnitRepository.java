package kr.ac.hansung.smartrent.domain.equipment.repository;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentUnitRepository extends JpaRepository<EquipmentUnit, Long> {
}
