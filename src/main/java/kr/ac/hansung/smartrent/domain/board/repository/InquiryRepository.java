package kr.ac.hansung.smartrent.domain.board.repository;

import java.util.List;

import kr.ac.hansung.smartrent.domain.board.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	List<Inquiry> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
