package kr.ac.hansung.smartrent.global.storage;

import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * 운영용: 비공개 S3 버킷에 저장(STORAGE_MODE=s3, 외부 연결 5절).
 * 자격 증명은 AWS 기본 방식으로 찾습니다(EC2는 IAM 역할, 개인 컴퓨터는 AWS CLI 로그인). 액세스 키를 .env에 넣지 않습니다.
 */
@Slf4j
public class S3StorageService implements StorageService {

	private final S3Client client;
	private final S3Presigner presigner;
	private final String bucket;

	public S3StorageService(S3Client client, S3Presigner presigner, String bucket) {
		this.client = client;
		this.presigner = presigner;
		this.bucket = bucket;
	}

	@Override
	public StorageMode mode() {
		return StorageMode.S3;
	}

	@Override
	public void put(String key, byte[] content, String contentType) {
		try {
			client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
				RequestBody.fromBytes(content));
		}
		catch (SdkException e) {
			log.warn("S3에 올리지 못했습니다: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
		}
	}

	/** S3 삭제는 없는 키여도 성공으로 답합니다 */
	@Override
	public void delete(String key) {
		try {
			client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
		}
		catch (SdkException e) {
			throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
		}
	}

	@Override
	public String temporaryUrl(String key) {
		GetObjectPresignRequest request = GetObjectPresignRequest.builder()
			.signatureDuration(URL_LIFETIME)
			.getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
			.build();
		return presigner.presignGetObject(request).url().toString();
	}
}
