package kr.ac.hansung.smartrent.domain.equipment.repository;

import java.util.List;
import java.util.Optional;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {
	/** 운영 중인 모델(운영 종료 제외) */
	List<EquipmentModel> findAllByArchivedFalseOrderByIdAsc();

	/** 이름 키(소문자, 공백 제거)가 같은 운영 중 모델. 운영 중 모델끼리는 이름 키가 겹치지 않아 하나 이하 */
	Optional<EquipmentModel> findByNameKeyAndArchivedFalse(String nameKey);
}
