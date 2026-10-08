package kr.ac.hansung.smartrent.domain.equipment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.service.ModelImageService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 관리자 권한은 SecurityConfig(/api/v1/admin/**)에서 확인 */
@Tag(name = "관리자 기자재")
@RestController
@RequestMapping("/api/v1/admin/models")
@RequiredArgsConstructor
public class AdminModelImageController {

	private final ModelImageService modelImageService;

	@Operation(summary = "모델 사진 올리기")
	@PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<AdminModelDetailResponse> add(@PathVariable Long id,
		@RequestPart(name = "image", required = false) MultipartFile image) {
		return ApiResponse.ok(modelImageService.add(id, image));
	}
}
