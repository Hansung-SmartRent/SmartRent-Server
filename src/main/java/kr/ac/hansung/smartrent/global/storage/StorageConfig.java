package kr.ac.hansung.smartrent.global.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

import kr.ac.hansung.smartrent.global.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** STORAGE_MODE로 저장소 구현 고르기. s3인데 S3_BUCKET이 비어 있으면 서버가 켜지지 않습니다 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

	@Bean
	public StorageService storageService(StorageProperties properties, JwtProperties jwtProperties, Clock clock) {
		if (properties.mode() == StorageMode.S3) {
			if (!StringUtils.hasText(properties.s3Bucket())) {
				throw new IllegalStateException("STORAGE_MODE=s3이면 S3_BUCKET이 필요합니다.");
			}
			Region region = Region.of(properties.awsRegion());
			AwsCredentialsProvider credentials = credentials(properties);
			return new S3StorageService(S3Client.builder().region(region).credentialsProvider(credentials).build(),
				S3Presigner.builder().region(region).credentialsProvider(credentials).build(), properties.s3Bucket());
		}
		Path root = StringUtils.hasText(properties.localDir()) ? Path.of(properties.localDir())
			: Path.of(System.getProperty("user.home"), "smartrent-uploads");
		// 개발용 임시 주소 서명 키. JWT 키를 그대로 쓰지 않고 용도를 붙여 구분
		byte[] signingKey = ("local-file:" + jwtProperties.secret()).getBytes(StandardCharsets.UTF_8);
		return new LocalStorageService(root, signingKey, clock);
	}

	/**
	 * .env에 키가 있으면 그 키로, 둘 다 비어 있으면 EC2 IAM 역할로만 접속합니다.
	 * SDK 기본 인증은 .env를 보지 않고 ~/.aws 프로필로 넘어가므로 쓰지 않습니다.
	 */
	static AwsCredentialsProvider credentials(StorageProperties properties) {
		boolean hasId = StringUtils.hasText(properties.awsAccessKeyId());
		boolean hasSecret = StringUtils.hasText(properties.awsSecretAccessKey());
		if (hasId != hasSecret) {
			throw new IllegalStateException("AWS_ACCESS_KEY_ID와 AWS_SECRET_ACCESS_KEY는 둘 다 넣거나 둘 다 비워야 합니다.");
		}
		if (hasId) {
			return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.awsAccessKeyId(), properties.awsSecretAccessKey()));
		}
		return InstanceProfileCredentialsProvider.create();
	}
}
