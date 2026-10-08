package kr.ac.hansung.smartrent.global.audit;

import kr.ac.hansung.smartrent.global.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
