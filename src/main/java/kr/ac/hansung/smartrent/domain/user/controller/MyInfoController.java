package kr.ac.hansung.smartrent.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ac.hansung.smartrent.domain.user.dto.EmailChangeRequest;
import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;
import kr.ac.hansung.smartrent.domain.user.dto.PasswordChangeRequest;
import kr.ac.hansung.smartrent.domain.user.service.MyInfoService;
import kr.ac.hansung.smartrent.domain.user.service.ProfileImageService;
import kr.ac.hansung.smartrent.global.response.ApiResponse;
import kr.ac.hansung.smartrent.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "내 정보")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyInfoController {

	private final MyInfoService myInfoService;
	private final ProfileImageService profileImageService;

	@Operation(summary = "내 정보")
	@GetMapping
	public ApiResponse<MeResponse> me() {
		return ApiResponse.ok(myInfoService.me(CurrentUser.id()));
	}

	@Operation(summary = "비밀번호 변경")
	@PutMapping("/password")
	public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		myInfoService.changePassword(CurrentUser.id(), request);
		return ApiResponse.ok(null);
	}

	@Operation(summary = "이메일 변경")
	@PutMapping("/email")
	public ApiResponse<MeResponse> changeEmail(@Valid @RequestBody EmailChangeRequest request) {
		return ApiResponse.ok(myInfoService.changeEmail(CurrentUser.id(), request));
	}

	@Operation(summary = "프로필 사진 바꾸기")
	@PutMapping(path = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<MeResponse> changeProfileImage(@RequestPart(name = "image", required = false) MultipartFile image) {
		return ApiResponse.ok(profileImageService.change(CurrentUser.id(), image));
	}

	@Operation(summary = "프로필 사진 지우기")
	@DeleteMapping("/profile-image")
	public ApiResponse<MeResponse> removeProfileImage() {
		return ApiResponse.ok(profileImageService.remove(CurrentUser.id()));
	}
}
