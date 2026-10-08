package kr.ac.hansung.smartrent.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml smartrent.storage (.env의 STORAGE_MODE, STORAGE_LOCAL_DIR, S3_BUCKET, AWS_REGION) */
@ConfigurationProperties(prefix = "smartrent.storage")
public record StorageProperties(StorageMode mode, String localDir, String s3Bucket, String awsRegion) {
}
