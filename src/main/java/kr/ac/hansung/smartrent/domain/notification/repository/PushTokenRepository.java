package kr.ac.hansung.smartrent.domain.notification.repository;
import java.util.*;
import kr.ac.hansung.smartrent.domain.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PushTokenRepository extends JpaRepository<PushToken,Long> {
 Optional<PushToken> findByToken(String token);
 List<PushToken> findByUserId(Long userId);
 void deleteByUserIdAndToken(Long userId,String token);
}
