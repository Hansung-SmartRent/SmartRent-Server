package kr.ac.hansung.smartrent.domain.equipment.repository;

import java.util.List;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {
	/** 운영 중인 모델(운영 종료 제외) */
	List<EquipmentModel> findAllByArchivedFalseOrderByIdAsc();
}
