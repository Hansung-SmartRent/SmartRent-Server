package kr.ac.hansung.smartrent.support;
import java.time.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.notification.service.PushSender;
import kr.ac.hansung.smartrent.domain.notification.dto.NotificationResponse;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest(properties={"smartrent.rental.scheduler-enabled=false"})
@AutoConfigureMockMvc @ActiveProfiles("local")
@Import({TestcontainersConfiguration.class,Backend2Support.Fakes.class})
public abstract class Backend2Support {
 public static final Instant NOW=OffsetDateTime.parse("2026-10-07T09:00:00+09:00").toInstant();
 @TestConfiguration public static class Fakes {
  @Bean @Primary MutableClock backend2Clock() { return new MutableClock(NOW); }
  @Bean @Primary FakePush backend2Push() { return new FakePush(); }
 }
 public static class FakePush implements PushSender {
  public int calls;
  public Result result=Result.SENT;
  public Result send(String token,NotificationResponse notification) { calls++; return result; }
 }
 @Autowired protected MockMvc mvc;
 @Autowired protected EntityManager em;
 @Autowired protected MutableClock clock;
 @Autowired protected FakePush push;
 @Autowired protected UserRepository users;
 @Autowired protected JwtEncoder encoder;
 @BeforeEach void backend2Reset() { clock.set(NOW); push.calls=0; push.result=PushSender.Result.SENT; }
 protected Long student() { return users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId(); }
 protected Long other() { return users.findByEmail("minjun@hansung.ac.kr").orElseThrow().getId(); }
 protected Long admin() { return users.findByEmail("admin@hansung.ac.kr").orElseThrow().getId(); }
 protected String token(Long id,String role) { return TestTokens.bearer(encoder,id,role,clock.instant().plusSeconds(1800)); }
 protected String studentToken() { return token(student(),"STUDENT"); }
 protected String adminToken() { return token(admin(),"ADMIN"); }
 protected void at(String at) { clock.set(OffsetDateTime.parse(at).toInstant()); }
}
