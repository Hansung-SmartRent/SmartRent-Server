package kr.ac.hansung.smartrent.domain.operation.controller;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.operation.dto.HolidayResponse;
import kr.ac.hansung.smartrent.domain.operation.dto.HolidayWriteRequest;
import kr.ac.hansung.smartrent.domain.operation.dto.OperationChangeResponse;
import kr.ac.hansung.smartrent.domain.operation.service.HolidayService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HolidayController {

	private final HolidayService holidayService;

	@Operation(summary = "휴무일 목록")
	@GetMapping("/holidays")
	public ApiResponse<List<HolidayResponse>> list(@RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.ok(holidayService.list(from, to));
	}

	@Operation(summary = "임시 휴무 추가·공휴일 운영 해제 취소")
	@PostMapping("/admin/holidays")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<OperationChangeResponse> close(@Valid @RequestBody HolidayWriteRequest request) {
		return ApiResponse.ok(holidayService.close(request, CurrentUser.id()));
	}

	@Operation(summary = "공휴일에 운영하기")
	@PostMapping("/admin/holidays/{date}/open")
	public ApiResponse<HolidayResponse> open(@PathVariable LocalDate date) {
		return ApiResponse.ok(holidayService.open(date, CurrentUser.id()));
	}

	@Operation(summary = "임시 휴무 지우기")
	@DeleteMapping("/admin/holidays/{date}")
	public ApiResponse<Void> delete(@PathVariable LocalDate date) {
		holidayService.delete(date, CurrentUser.id());
		return ApiResponse.ok(null);
	}
}
