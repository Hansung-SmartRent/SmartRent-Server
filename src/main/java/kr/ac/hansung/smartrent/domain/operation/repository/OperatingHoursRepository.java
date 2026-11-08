package kr.ac.hansung.smartrent.domain.operation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import kr.ac.hansung.smartrent.domain.operation.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Long> {

	List<OperatingHours> findAllByModelId(Long modelId);

	Optional<OperatingHours> findByModelIdAndDateIsNull(Long modelId);

	Optional<OperatingHours> findByModelIdAndDate(Long modelId, LocalDate date);
}
