package kr.ac.hansung.smartrent.global.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 개발용: 레포 밖 로컬 폴더에 저장(STORAGE_MODE=local).
 * S3의 임시 주소처럼 만료 시각과 서명을 붙인 주소를 주고, LocalFileController가 서명과 시각을 확인한 뒤 파일을 줍니다.
 */
@Slf4j
public class LocalStorageService implements StorageService {

	public static final String URL_PREFIX = "/api/v1/files/local/";

	private final Path root;
	private final byte[] signingKey;
	private final Clock clock;

	public LocalStorageService(Path root, byte[] signingKey, Clock clock) {
		this.root = root.toAbsolutePath().normalize();
		this.signingKey = signingKey;
		this.clock = clock;
	}

	@Override
	public StorageMode mode() {
		return StorageMode.LOCAL;
	}

	@Override
	public void put(String key, byte[] content, String contentType) {
		Path path = resolve(key);
		try {
			Files.createDirectories(path.getParent());
			Files.write(path, content);
		}
		catch (IOException e) {
			log.warn("로컬 저장소에 쓰지 못했습니다: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		}
		catch (IOException e) {
			throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
		}
	}

	@Override
	public String temporaryUrl(String key) {
		long expires = clock.instant().plus(URL_LIFETIME).getEpochSecond();
		String signature = sign(key, expires);
		try {
			return ServletUriComponentsBuilder.fromCurrentContextPath().path(URL_PREFIX + key)
				.queryParam("expires", expires).queryParam("signature", signature).toUriString();
		}
		catch (IllegalStateException e) {
			return URL_PREFIX + key + "?expires=" + expires + "&signature=" + signature;
		}
	}

	/** 서명이 맞고 만료 전이면 파일 내용 */
	@Override
	public Optional<byte[]> readSigned(String key, long expires, String signature) {
		boolean signed = MessageDigest.isEqual(sign(key, expires).getBytes(StandardCharsets.US_ASCII),
			signature.getBytes(StandardCharsets.US_ASCII));
		if (!signed || clock.instant().getEpochSecond() > expires) {
			return Optional.empty();
		}
		try {
			Path path = resolve(key);
			return Files.exists(path) ? Optional.of(Files.readAllBytes(path)) : Optional.empty();
		}
		catch (IOException e) {
			return Optional.empty();
		}
	}

	/** 키가 저장 폴더 밖을 가리키지 못하게 막음 */
	private Path resolve(String key) {
		Path path = root.resolve(key).normalize();
		if (!path.startsWith(root) || path.equals(root)) {
			throw new BusinessException(ErrorCode.NOT_FOUND);
		}
		return path;
	}

	private String sign(String key, long expires) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal((key + "\n" + expires).getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
