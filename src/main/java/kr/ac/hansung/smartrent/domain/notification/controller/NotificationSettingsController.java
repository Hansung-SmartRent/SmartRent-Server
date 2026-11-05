package kr.ac.hansung.smartrent.domain.notification.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationSettingsService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/users/me/notification-settings")
public class NotificationSettingsController {
 private final NotificationSettingsService service;
 @GetMapping public ApiResponse<Settings> get() { return ApiResponse.ok(new Settings(service.dueEnabled(CurrentUser.id()))); }
 @PutMapping public ApiResponse<Settings> change(@Valid @RequestBody Settings request) {
  service.change(CurrentUser.id(),request.dueEnabled()); return ApiResponse.ok(request);
 }
 public record Settings(@NotNull Boolean dueEnabled) { }
}
