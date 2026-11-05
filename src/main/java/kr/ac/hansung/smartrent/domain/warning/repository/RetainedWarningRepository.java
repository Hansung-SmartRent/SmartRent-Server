package kr.ac.hansung.smartrent.domain.warning.repository;
import java.util.List;
import kr.ac.hansung.smartrent.domain.warning.entity.RetainedWarning;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RetainedWarningRepository extends JpaRepository<RetainedWarning,Long> {
 List<RetainedWarning> findByStudentNumber(String number);
}
