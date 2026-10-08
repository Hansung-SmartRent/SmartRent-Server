package kr.ac.hansung.smartrent.global.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배포 확인용. API 명세 밖입니다(B1-01). */
@Tag(name = "헬스 체크")
@RestController
public class HealthController {

	@Operation(summary = "서버가 켜져 있는지 확인")
	@GetMapping("/api/v1/health")
	public ApiResponse<Void> health() {
		return ApiResponse.ok("서버가 동작 중입니다.", null);
	}
}
