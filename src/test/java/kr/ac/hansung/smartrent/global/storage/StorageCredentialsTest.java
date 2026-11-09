package kr.ac.hansung.smartrent.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/** S3 접속 키 고르기. .env의 키 또는 EC2 IAM 역할만 쓰고 ~/.aws 프로필로 넘어가지 않아야 함 */
class StorageCredentialsTest {

	private StorageProperties properties(String id, String secret) {
		return new StorageProperties(StorageMode.S3, null, "bucket", "ap-southeast-2", id, secret);
	}

	@Test
	void 두_키가_모두_있으면_그_키로_접속() {
		var provider = StorageConfig.credentials(properties("test-id", "test-secret"));

		assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
		assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("test-id");
		assertThat(provider.resolveCredentials().secretAccessKey()).isEqualTo("test-secret");
	}

	@Test
	void 두_키가_모두_비어_있으면_EC2_IAM_역할로만_접속() {
		assertThat(StorageConfig.credentials(properties("", "")))
			.isExactlyInstanceOf(InstanceProfileCredentialsProvider.class);
		assertThat(StorageConfig.credentials(properties(null, null)))
			.isExactlyInstanceOf(InstanceProfileCredentialsProvider.class);
	}

	@Test
	void 키가_하나만_있으면_서버가_켜지지_않음() {
		assertThatThrownBy(() -> StorageConfig.credentials(properties("test-id", "")))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> StorageConfig.credentials(properties("", "test-secret")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 설정값을_문자열로_출력해도_키는_보이지_않음() {
		assertThat(properties("test-id", "test-secret").toString()).doesNotContain("test-id", "test-secret");
	}
}
