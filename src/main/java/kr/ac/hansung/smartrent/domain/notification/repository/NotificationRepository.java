package kr.ac.hansung.smartrent.domain.notification.repository;
import java.util.Optional;
import kr.ac.hansung.smartrent.domain.notification.entity.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface NotificationRepository extends JpaRepository<Notification,Long> {
 Page<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
 Optional<Notification> findByIdAndUserId(Long id, Long userId);
 @Modifying @Query("update Notification n set n.read=true where n.userId=:userId and n.read=false")
 int markAllRead(Long userId);
}
