package kr.ac.hansung.smartrent.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.storage.LocalStorageService;
import kr.ac.hansung.smartrent.global.storage.StorageMode;
import kr.ac.hansung.smartrent.global.storage.StorageService;

/** 시험용 저장소: 임시 폴더의 local 저장소에, 올리기·지우기를 일부러 실패시키는 스위치를 붙임 */
public class FlakyStorage implements StorageService {

	public final Path root;
	public final LocalStorageService local;
	public volatile boolean failPut;
	public volatile boolean failDelete;

	public FlakyStorage(Clock clock) {
		try {
			this.root = Files.createTempDirectory("smartrent-test-uploads");
		}
		catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
		this.local = new LocalStorageService(root, "test-signing-key".getBytes(), clock);
	}

	public boolean exists(String key) {
		return Files.exists(root.resolve(key));
	}

	@Override
	public StorageMode mode() {
		return StorageMode.LOCAL;
	}

	@Override
	public void put(String key, byte[] content, String contentType) {
		if (failPut) {
			throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
		}
		local.put(key, content, contentType);
	}

	@Override
	public void delete(String key) {
		if (failDelete) {
			throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
		}
		local.delete(key);
	}

	@Override
	public String temporaryUrl(String key) {
		return local.temporaryUrl(key);
	}

	@Override
	public java.util.Optional<byte[]> readSigned(String key, long expires, String signature) {
		return local.readSigned(key, expires, signature);
	}
}
