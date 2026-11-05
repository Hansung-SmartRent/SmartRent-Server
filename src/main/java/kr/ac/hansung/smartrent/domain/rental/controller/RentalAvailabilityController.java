package kr.ac.hansung.smartrent.domain.rental.controller;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.hansung.smartrent.domain.rental.dto.AvailabilityResponse;
import kr.ac.hansung.smartrent.domain.rental.service.RentalAvailabilityService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기자재")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/models")
public class RentalAvailabilityController {
	private final RentalAvailabilityService service;

	@Operation(summary = "예약 가능한 칸")
	@GetMapping("/{id}/availability")
	public ApiResponse<AvailabilityResponse> availability(@PathVariable long id,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.ok(service.availability(id, date));
	}
}
