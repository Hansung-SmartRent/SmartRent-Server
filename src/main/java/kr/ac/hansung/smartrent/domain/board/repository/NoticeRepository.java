package kr.ac.hansung.smartrent.domain.board.repository;

import kr.ac.hansung.smartrent.domain.board.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

	/** 최신순(기획안 11장) */
	Page<Notice> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
