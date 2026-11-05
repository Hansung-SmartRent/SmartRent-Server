package kr.ac.hansung.smartrent.domain.warning.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.warning.service.WarningService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
public class WarningController {
 private final WarningService service;
 @GetMapping("/users/me/warnings") @PreAuthorize("hasRole('STUDENT')")
 public ApiResponse<?> mine() { return ApiResponse.ok(service.mine(CurrentUser.id())); }
 @PostMapping("/admin/warnings/{id}/revoke")
 public ApiResponse<Void> revoke(@PathVariable Long id,@Valid @RequestBody Reason request) {
  service.revoke(id,request.reason(),CurrentUser.id()); return ApiResponse.ok(null);
 }
 public record Reason(@NotBlank @Size(max=100) String reason) { }
}
