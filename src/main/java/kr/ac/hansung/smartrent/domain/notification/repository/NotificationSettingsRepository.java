package kr.ac.hansung.smartrent.domain.notification.repository;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings,Long> { }
