package kr.ac.hansung.smartrent.domain.rental.repository;

import kr.ac.hansung.smartrent.domain.rental.entity.RentalApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalApplicationRepository extends JpaRepository<RentalApplication, Long> {
}
