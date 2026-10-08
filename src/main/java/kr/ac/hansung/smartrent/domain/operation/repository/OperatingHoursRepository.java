package kr.ac.hansung.smartrent.domain.operation.repository;

import kr.ac.hansung.smartrent.domain.operation.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Long> {
}
