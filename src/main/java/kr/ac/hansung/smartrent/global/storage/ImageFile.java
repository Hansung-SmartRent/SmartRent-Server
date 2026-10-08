package kr.ac.hansung.smartrent.global.storage;

import java.io.IOException;
import java.util.Arrays;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

/** 올라온 사진 확인: jpg·png, 10MB 이하(확정). 확장자나 Content-Type이 아니라 파일 앞부분으로 형식을 봅니다 */
public record ImageFile(byte[] content, String contentType, String extension) {

	public static final long MAX_BYTES = 10L * 1024 * 1024;

	private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

	public static ImageFile from(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
			throw new BusinessException(ErrorCode.FILE_INVALID);
		}
		byte[] content;
		try {
			content = file.getBytes();
		}
		catch (IOException e) {
			throw new BusinessException(ErrorCode.FILE_INVALID);
		}
		if (startsWith(content, JPG)) {
			return new ImageFile(content, "image/jpeg", "jpg");
		}
		if (startsWith(content, PNG)) {
			return new ImageFile(content, "image/png", "png");
		}
		throw new BusinessException(ErrorCode.FILE_INVALID);
	}

	private static boolean startsWith(byte[] content, byte[] head) {
		return content.length >= head.length && Arrays.equals(content, 0, head.length, head, 0, head.length);
	}
}
