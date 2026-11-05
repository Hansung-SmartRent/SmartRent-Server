package kr.ac.hansung.smartrent.domain.rental.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kr.ac.hansung.smartrent.domain.rental.service.ReservationService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/reservations")
public class ReservationController {
 private final ReservationService service;
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')") @RequireApprovedStudent
 public ApiResponse<ReservationService.Result> reserve(@RequestBody ReservationService.Request request) {
  return ApiResponse.ok(service.reserve(CurrentUser.id(),request));
 }
}
