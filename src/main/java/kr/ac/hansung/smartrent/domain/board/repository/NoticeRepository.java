package kr.ac.hansung.smartrent.domain.board.repository;

import kr.ac.hansung.smartrent.domain.board.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
