package kr.ac.hansung.smartrent.domain.warning.repository;
import java.util.*;
import kr.ac.hansung.smartrent.domain.warning.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WarningRepository extends JpaRepository<Warning,Long> {
 List<Warning> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
 Optional<Warning> findByRentalIdAndKind(Long rentalId,WarningKind kind);
}
