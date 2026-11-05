package kr.ac.hansung.smartrent.domain.rental.controller;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.rental.service.RentalCorrectionService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/rentals")
public class RentalCorrectionController {
 private final RentalCorrectionService service;
 @PostMapping("/{id}/correct-no-show") public ApiResponse<?> noShow(@PathVariable Long id,@Valid @RequestBody NoShow r) {
  return ApiResponse.ok(service.noShow(id,r.unitCode(),r.actualPickedAt(),Boolean.TRUE.equals(r.professorMailChecked()),CurrentUser.id()));
 }
 @PostMapping("/{id}/correct-return") public ApiResponse<?> returned(@PathVariable Long id,@Valid @RequestBody Returned r) {
  return ApiResponse.ok(service.returned(id,r.actualReturnedAt(),CurrentUser.id()));
 }
 @PostMapping("/{id}/damage-discovered") public ApiResponse<?> damage(@PathVariable Long id,@Valid @RequestBody Damage r) {
  return ApiResponse.ok(service.damage(id,r.note(),CurrentUser.id()));
 }
 public record NoShow(@NotBlank String unitCode,@NotNull LocalDateTime actualPickedAt,Boolean professorMailChecked) { }
 public record Returned(@NotNull LocalDateTime actualReturnedAt) { }
 public record Damage(@NotBlank @Size(max=500) String note) { }
}
