package kr.ac.hansung.smartrent.domain.notification.config;
import java.io.IOException;
import java.nio.file.*;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import kr.ac.hansung.smartrent.domain.notification.service.PushSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class FcmConfig {
 @Bean
 PushSender pushSender(@Value("${smartrent.fcm.enabled:false}") boolean enabled,
  @Value("${smartrent.fcm.credentials-path:}") String credentialsPath) throws IOException {
  if (!enabled) return (token, notification) -> PushSender.Result.FAILED;
  if (credentialsPath.isBlank()) throw new IllegalStateException("FCM_CREDENTIALS_PATH가 필요합니다.");
  FirebaseApp app;
  try (var in=Files.newInputStream(Path.of(credentialsPath))) {
   app=FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(ServiceAccountCredentials.fromStream(in))
    .setConnectTimeout(5000).setReadTimeout(5000).build(),
    "smartrent-"+java.util.UUID.randomUUID());
  }
  return new FirebasePushSender(app);
 }
 static final class FirebasePushSender implements PushSender, AutoCloseable {
  private final FirebaseApp app;
  FirebasePushSender(FirebaseApp app) { this.app=app; }
  public Result send(String token, kr.ac.hansung.smartrent.domain.notification.dto.NotificationResponse n) {
   var builder=Message.builder().setToken(token)
    .setNotification(com.google.firebase.messaging.Notification.builder().setTitle(n.title()).setBody(n.body()).build())
    .putData("kind",n.kind().name());
   if(n.relatedType()!=null) builder.putData("relatedType",n.relatedType().name()).putData("relatedId",n.relatedId().toString());
   try { FirebaseMessaging.getInstance(app).send(builder.build()); return Result.SENT; }
   catch(FirebaseMessagingException ex) {
    return ex.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED ? Result.UNREGISTERED : Result.FAILED;
   }
  }
  public void close() { app.delete(); }
 }
}
