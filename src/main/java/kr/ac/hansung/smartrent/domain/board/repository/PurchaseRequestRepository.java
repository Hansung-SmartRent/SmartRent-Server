package kr.ac.hansung.smartrent.domain.board.repository;

import kr.ac.hansung.smartrent.domain.board.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
}
