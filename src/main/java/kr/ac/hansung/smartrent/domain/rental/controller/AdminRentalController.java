package kr.ac.hansung.smartrent.domain.rental.controller;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.service.AdminRentalService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @Validated @RequestMapping("/api/v1/admin")
public class AdminRentalController {
 private final AdminRentalService service;
 @GetMapping("/summary") public ApiResponse<?> summary() { return ApiResponse.ok(service.summary()); }
 @GetMapping("/rentals") public ApiResponse<?> list(@RequestParam(required=false) RentalStatus status,@RequestParam(required=false) String q,
  @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
  return ApiResponse.ok(service.list(status,q,page,size));
 }
}
