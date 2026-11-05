package kr.ac.hansung.smartrent.domain.notification.controller;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.notification.dto.NotificationResponse;
import kr.ac.hansung.smartrent.domain.notification.entity.PushPlatform;
import kr.ac.hansung.smartrent.domain.notification.repository.NotificationRepository;
import kr.ac.hansung.smartrent.domain.notification.service.NotificationService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @Validated
@RequestMapping("/api/v1")
public class NotificationController {
 private final NotificationService service;
 private final NotificationRepository repository;
 @GetMapping("/notifications")
 public ApiResponse<?> list(@RequestParam(defaultValue="0") @Min(0) int page,
  @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
  var result=repository.findByUserIdOrderByCreatedAtDescIdDesc(CurrentUser.id(),PageRequest.of(page,size));
  return ApiResponse.ok(Map.of("items",result.map(NotificationResponse::of).getContent(),"page",
   Map.of("page",page,"size",size,"totalElements",result.getTotalElements(),"totalPages",result.getTotalPages())));
 }
 @PostMapping("/notifications/{id}/read") public ApiResponse<Void> read(@PathVariable Long id) {
  service.read(CurrentUser.id(),id); return ApiResponse.ok(null);
 }
 @PostMapping("/notifications/read-all") public ApiResponse<Void> readAll() {
  service.readAll(CurrentUser.id()); return ApiResponse.ok(null);
 }
 @PutMapping("/users/me/push-token") public ApiResponse<Void> register(@Valid @RequestBody TokenRequest request) {
  service.register(CurrentUser.id(),request.token(),request.platform()); return ApiResponse.ok(null);
 }
 public record TokenRequest(@NotBlank @Size(max=512) String token,@NotNull PushPlatform platform) { }
}
