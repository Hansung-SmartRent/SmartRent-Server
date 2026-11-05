package kr.ac.hansung.smartrent.global.port;
import org.springframework.stereotype.Component;
/** B2-17 보관 경고 복원 연결 전 기본 구현. */
@Component
public class NoWarningsYet implements RetainedWarningRestorer {
 public void restore(Long userId,String studentNumber) { }
}
