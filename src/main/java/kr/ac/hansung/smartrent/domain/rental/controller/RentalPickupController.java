package kr.ac.hansung.smartrent.domain.rental.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kr.ac.hansung.smartrent.domain.rental.service.RentalPickupService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/rentals")
public class RentalPickupController {
 private final RentalPickupService service;
 @PostMapping("/{id}/pickup") public ApiResponse<?> pickup(@PathVariable Long id,@Valid @RequestBody Request request) {
  return ApiResponse.ok(service.pickup(id,request.unitCode(),request.professorMailChecked(),CurrentUser.id()));
 }
 public record Request(@NotBlank String unitCode,boolean professorMailChecked) { }
}
