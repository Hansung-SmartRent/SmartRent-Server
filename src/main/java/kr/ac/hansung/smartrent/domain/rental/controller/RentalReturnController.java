package kr.ac.hansung.smartrent.domain.rental.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.rental.entity.ReturnCondition;
import kr.ac.hansung.smartrent.domain.rental.service.RentalReturnService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/rentals")
public class RentalReturnController {
 private final RentalReturnService service;
 @PostMapping("/{id}/return") public ApiResponse<?> returnRental(@PathVariable Long id,@Valid @RequestBody Request request) {
  return ApiResponse.ok(service.returnRental(id,request.unitCode(),request.condition(),CurrentUser.id()));
 }
 public record Request(@NotBlank String unitCode,@NotNull ReturnCondition condition) { }
}
