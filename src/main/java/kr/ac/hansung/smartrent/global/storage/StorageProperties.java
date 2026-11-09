package kr.ac.hansung.smartrent.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml smartrent.storage (.env의 STORAGE_MODE, STORAGE_LOCAL_DIR, S3_BUCKET, AWS_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) */
@ConfigurationProperties(prefix = "smartrent.storage")
public record StorageProperties(StorageMode mode, String localDir, String s3Bucket, String awsRegion,
	String awsAccessKeyId, String awsSecretAccessKey) {

	/** 키가 로그·예외 메시지에 찍히지 않게 기록에서 숨깁니다 */
	@Override
	public String toString() {
		return "StorageProperties[mode=" + mode + ", s3Bucket=" + s3Bucket + ", awsRegion=" + awsRegion + "]";
	}
}
