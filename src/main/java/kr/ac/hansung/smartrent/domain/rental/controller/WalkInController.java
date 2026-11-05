package kr.ac.hansung.smartrent.domain.rental.controller;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ac.hansung.smartrent.domain.rental.service.WalkInService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/walk-ins")
public class WalkInController {
 private final WalkInService service;
 @GetMapping("/options") public ApiResponse<?> options(@RequestParam String studentNumber,@RequestParam Long modelId) {
  return ApiResponse.ok(service.options(studentNumber,modelId));
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> walkIn(@Valid @RequestBody Request r) {
  return ApiResponse.ok(service.walkIn(r.studentNumber(),r.modelId(),r.unitCode(),r.end(),Boolean.TRUE.equals(r.professorMailChecked()),CurrentUser.id()));
 }
 public record Request(@NotBlank String studentNumber,@NotNull Long modelId,@NotBlank String unitCode,
  @NotNull LocalDateTime end,Boolean professorMailChecked) { }
}
