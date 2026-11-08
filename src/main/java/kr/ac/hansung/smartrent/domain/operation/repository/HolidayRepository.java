package kr.ac.hansung.smartrent.domain.operation.repository;

import java.time.LocalDate;
import java.util.List;

import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

	List<Holiday> findAllByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
