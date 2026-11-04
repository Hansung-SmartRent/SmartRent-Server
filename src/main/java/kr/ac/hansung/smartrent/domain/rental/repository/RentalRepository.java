package kr.ac.hansung.smartrent.domain.rental.repository;

import java.util.Collection;
import java.util.List;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    List<Rental> findAllByModelIdAndStatusIn(Long modelId, Collection<RentalStatus> statuses);
}
