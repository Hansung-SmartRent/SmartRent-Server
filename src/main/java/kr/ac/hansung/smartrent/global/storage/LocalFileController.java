package kr.ac.hansung.smartrent.global.storage;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STORAGE_MODE=local일 때 임시 주소로 파일 주기(개발용). 로그인 없이 열리고 서명·만료 시각으로만 확인합니다.
 * 서명이 틀리거나 10분이 지났거나 s3 모드면 404
 */
@RestController
@RequiredArgsConstructor
public class LocalFileController {

	private final StorageService storageService;

	@GetMapping(LocalStorageService.URL_PREFIX + "**")
	public ResponseEntity<byte[]> file(HttpServletRequest request, @RequestParam long expires,
		@RequestParam String signature) {
		String key = request.getRequestURI().substring(request.getContextPath().length() + LocalStorageService.URL_PREFIX.length());
		byte[] content = storageService.readSigned(key, expires, signature).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		MediaType type = key.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok().contentType(type).body(content);
	}
}
