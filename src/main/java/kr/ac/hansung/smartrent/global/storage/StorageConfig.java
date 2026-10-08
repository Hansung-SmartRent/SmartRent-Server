package kr.ac.hansung.smartrent.global.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

import kr.ac.hansung.smartrent.global.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
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
			return new S3StorageService(S3Client.builder().region(region).build(),
				S3Presigner.builder().region(region).build(), properties.s3Bucket());
		}
		Path root = StringUtils.hasText(properties.localDir()) ? Path.of(properties.localDir())
			: Path.of(System.getProperty("user.home"), "smartrent-uploads");
		// 개발용 임시 주소 서명 키. JWT 키를 그대로 쓰지 않고 용도를 붙여 구분
		byte[] signingKey = ("local-file:" + jwtProperties.secret()).getBytes(StandardCharsets.UTF_8);
		return new LocalStorageService(root, signingKey, clock);
	}
}
