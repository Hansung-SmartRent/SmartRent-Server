package kr.ac.hansung.smartrent.domain.warning.service;
import java.util.List;
import kr.ac.hansung.smartrent.domain.rental.service.RentalReader;
import kr.ac.hansung.smartrent.global.port.StudentHistoryReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class StudentHistoryService implements StudentHistoryReader {
 private final WarningService warnings;
 private final RentalReader rentals;
 public List<?> warnings(Long id) { return warnings.items(id); }
 public List<?> rentals(Long id) { return rentals.mine(id,null); }
}
