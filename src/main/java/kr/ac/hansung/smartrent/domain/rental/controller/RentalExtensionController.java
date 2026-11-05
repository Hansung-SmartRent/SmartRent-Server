package kr.ac.hansung.smartrent.domain.rental.controller;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kr.ac.hansung.smartrent.domain.rental.service.RentalExtensionService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/rentals") @PreAuthorize("hasRole('STUDENT')")
public class RentalExtensionController {
 private final RentalExtensionService service;
 @GetMapping("/{id}/extension-options") public ApiResponse<?> options(@PathVariable Long id) {
  return ApiResponse.ok(service.options(id,CurrentUser.id()));
 }
 @PostMapping("/{id}/extend") @RequireApprovedStudent public ApiResponse<?> extend(@PathVariable Long id,@Valid @RequestBody Request r) {
  return ApiResponse.ok(service.extend(id,CurrentUser.id(),r.newEnd()));
 }
 public record Request(@NotNull LocalDateTime newEnd) { }
}
