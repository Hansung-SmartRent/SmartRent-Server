package kr.ac.hansung.smartrent.domain.rental.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCancelService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
public class RentalCancelController {
 private final RentalCancelService service;
 @PostMapping("/rentals/{id}/cancel") @PreAuthorize("hasRole('STUDENT')")
 public ApiResponse<Void> cancel(@PathVariable Long id) {
  service.cancel(id,CurrentUser.id(),false,"학생 취소"); return ApiResponse.ok(null);
 }
 @PostMapping("/admin/rentals/{id}/cancel") public ApiResponse<Void> cancelAdmin(@PathVariable Long id,@Valid @RequestBody Reason reason) {
  service.cancel(id,CurrentUser.id(),true,reason.reason()); return ApiResponse.ok(null);
 }
 public record Reason(@NotBlank @Size(max=100) String reason) { }
}
