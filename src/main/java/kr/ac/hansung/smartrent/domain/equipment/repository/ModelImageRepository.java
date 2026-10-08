package kr.ac.hansung.smartrent.domain.equipment.repository;

import kr.ac.hansung.smartrent.domain.equipment.entity.ModelImage;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelImageRepository extends JpaRepository<ModelImage, Long> {

	long countByModelId(Long modelId);

	List<ModelImage> findAllByModelIdOrderBySortOrderAsc(Long modelId);
}
