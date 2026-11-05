package kr.ac.hansung.smartrent.domain.rental.controller;
import kr.ac.hansung.smartrent.domain.rental.service.RentalLookupService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin")
public class RentalLookupController {
 private final RentalLookupService service;
 @GetMapping("/pickups/lookup") public ApiResponse<?> pickups(@RequestParam(required=false) String unitCode,@RequestParam(required=false) String studentNumber) {
  return ApiResponse.ok(service.pickups(unitCode,studentNumber));
 }
 @GetMapping("/returns/lookup") public ApiResponse<?> returns(@RequestParam String unitCode) { return ApiResponse.ok(service.returns(unitCode)); }
}
