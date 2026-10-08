package kr.ac.hansung.smartrent.global.storage;

/**
 * 사진 올리기·지우기·임시 주소 만들기(외부 연결 5절). local과 s3 구현 두 개 중 STORAGE_MODE로 하나를 씁니다.
 * 저장소에 닿지 못하면 BusinessException(STORAGE_UNAVAILABLE)을 던집니다.
 */
public interface StorageService {

	/** 앱에 주는 임시 주소의 유효 시간(확정: 10분) */
	java.time.Duration URL_LIFETIME = java.time.Duration.ofMinutes(10);

	StorageMode mode();

	void put(String key, byte[] content, String contentType);

	/** 이미 없는 파일이어도 성공으로 봅니다 */
	void delete(String key);

	/** 10분 동안만 열리는 주소 */
	String temporaryUrl(String key);

	/** local 모드의 임시 주소로 파일 읽기(서명·만료 확인). s3 모드는 S3가 직접 주므로 항상 비어 있음 */
	default java.util.Optional<byte[]> readSigned(String key, long expires, String signature) {
		return java.util.Optional.empty();
	}
}
