package kr.ac.hansung.smartrent.domain.rental.controller;
import kr.ac.hansung.smartrent.domain.rental.service.RentalReader;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/rentals")
public class RentalQueryController {
 private final RentalReader reader;
 @GetMapping("/me") @PreAuthorize("hasRole('STUDENT')")
 public ApiResponse<?> mine(@RequestParam(required=false) String group) { return ApiResponse.ok(reader.mine(CurrentUser.id(),group)); }
 @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id) {
  boolean admin=SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
  return ApiResponse.ok(reader.response(reader.owned(id,CurrentUser.id(),admin)));
 }
}
