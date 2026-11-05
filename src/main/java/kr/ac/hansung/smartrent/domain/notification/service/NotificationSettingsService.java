package kr.ac.hansung.smartrent.domain.notification.service;
import kr.ac.hansung.smartrent.domain.notification.entity.NotificationSettings;
import kr.ac.hansung.smartrent.domain.notification.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class NotificationSettingsService {
 private final NotificationSettingsRepository settings;
 @Transactional(readOnly=true)
 public boolean dueEnabled(Long userId) { return settings.findById(userId).map(NotificationSettings::isDueEnabled).orElse(true); }
 public void change(Long userId,boolean enabled) {
  var value=settings.findById(userId).orElseGet(()->NotificationSettings.builder().userId(userId).build());
  value.change(enabled); settings.save(value);
 }
}
