package kr.ac.hansung.smartrent.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;

import com.jayway.jsonpath.JsonPath;
import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.ModelImageRepository;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.support.FlakyStorage;
import kr.ac.hansung.smartrent.support.MutableClock;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** B1-08 서버 구현 후 확인할 것 1~7행. 저장소는 시험용 임시 폴더(FlakyStorage), 시각은 고정 Clock */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, PhotoStorageApiTest.Fakes.class})
class PhotoStorageApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();

	@TestConfiguration
	static class Fakes {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(NOW);
		}

		@Bean
		@Primary
		FlakyStorage flakyStorage(MutableClock clock) {
			return new FlakyStorage(clock);
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	MutableClock clock;
	@Autowired
	FlakyStorage storage;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	EquipmentModelRepository models;
	@Autowired
	ModelImageRepository modelImages;
	@Autowired
	StorageDeletionJobRepository jobs;
	@Autowired
	StorageDeletionService deletionService;

	@BeforeEach
	void reset() {
		clock.set(NOW);
		storage.failPut = false;
		storage.failDelete = false;
		jobs.deleteAll();
	}

	static byte[] png(int size) {
		byte[] b = new byte[size];
		byte[] head = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
		System.arraycopy(head, 0, b, 0, head.length);
		Arrays.fill(b, head.length, size, (byte) 7);
		return b;
	}

	static byte[] gif() {
		return "GIF89a.......".getBytes();
	}

	private User user(String email) {
		return users.findByEmail(email).orElseThrow();
	}

	private String tokenOf(String email, String role) {
		return TestTokens.bearer(jwtEncoder, user(email).getId(), role, NOW.plusSeconds(1800));
	}

	private ResultActions putProfile(String email, byte[] bytes, String name) throws Exception {
		return mvc.perform(multipart(HttpMethod.PUT, "/api/v1/users/me/profile-image")
			.file(new MockMultipartFile("image", name, "image/png", bytes))
			.header("Authorization", tokenOf(email, "STUDENT")));
	}

	@Test
	void B1_08_1_2_3_프로필_사진_바꾸기_다시_바꾸기_지우기() throws Exception {
		// 1행: png 5MB → 키 저장, 내 정보에 10분짜리 임시 주소
		putProfile("demo@hansung.ac.kr", png(5 * 1024 * 1024), "me.png").andExpect(status().isOk());
		String first = user("demo@hansung.ac.kr").getProfileImageKey();
		assertThat(first).startsWith("profile/").endsWith(".png");
		assertThat(storage.exists(first)).isTrue();
		String url = JsonPath.read(mvc.perform(get("/api/v1/users/me").header("Authorization", tokenOf("demo@hansung.ac.kr", "STUDENT")))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.profileImageUrl");
		long expires = Long.parseLong(url.replaceAll(".*expires=(\\d+).*", "$1"));
		assertThat(expires).isEqualTo(NOW.plusSeconds(600).getEpochSecond());

		// 2행: 한 번 더 바꾸면 옛 파일이 지워지고 삭제 작업도 남지 않음
		putProfile("demo@hansung.ac.kr", png(2048), "me2.png").andExpect(status().isOk());
		String second = user("demo@hansung.ac.kr").getProfileImageKey();
		assertThat(second).isNotEqualTo(first);
		assertThat(storage.exists(first)).isFalse();
		assertThat(storage.exists(second)).isTrue();
		assertThat(jobs.count()).isZero();

		// 3행: 지우면 키가 비고 파일도 삭제
		mvc.perform(delete("/api/v1/users/me/profile-image").header("Authorization", tokenOf("demo@hansung.ac.kr", "STUDENT")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());
		assertThat(user("demo@hansung.ac.kr").getProfileImageKey()).isNull();
		assertThat(storage.exists(second)).isFalse();
	}

	@Test
	void B1_08_4_11MB_사진이나_gif는_FILE_INVALID() throws Exception {
		putProfile("minjun@hansung.ac.kr", png(11 * 1024 * 1024), "big.png")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE_INVALID"));
		putProfile("minjun@hansung.ac.kr", gif(), "a.gif")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE_INVALID"));
		mvc.perform(multipart(HttpMethod.PUT, "/api/v1/users/me/profile-image").header("Authorization", tokenOf("minjun@hansung.ac.kr", "STUDENT")))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE_INVALID"));
		assertThat(user("minjun@hansung.ac.kr").getProfileImageKey()).isNull();
	}

	@Test
	void B1_08_5_저장소_쓰기가_실패하면_503이고_DB는_그대로() throws Exception {
		storage.failPut = true;
		putProfile("taeo@hansung.ac.kr", png(1024), "me.png")
			.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.errorCode").value("STORAGE_UNAVAILABLE"));
		assertThat(user("taeo@hansung.ac.kr").getProfileImageKey()).isNull();
	}

	@Test
	void B1_08_6_모델_사진은_3장까지_4번째는_VALIDATION_ERROR() throws Exception {
		EquipmentModel model = models.findAll().stream().filter(m -> modelImages.countByModelId(m.getId()) == 0).findFirst().orElseThrow();
		String admin = tokenOf("admin@hansung.ac.kr", "ADMIN");
		for (int i = 1; i <= 3; i++) {
			mvc.perform(multipart("/api/v1/admin/models/{id}/images", model.getId())
					.file(new MockMultipartFile("image", "m.png", "image/png", png(1024))).header("Authorization", admin))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.imageUrls.length()").value(i))
				.andExpect(jsonPath("$.data.id").value(model.getId()));
		}
		mvc.perform(multipart("/api/v1/admin/models/{id}/images", model.getId())
				.file(new MockMultipartFile("image", "m.png", "image/png", png(1024))).header("Authorization", admin))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		assertThat(modelImages.findAllByModelIdOrderBySortOrderAsc(model.getId())).extracting(i -> i.getSortOrder()).containsExactly(1, 2, 3);
		// 학생은 관리자 주소를 못 부름
		mvc.perform(multipart("/api/v1/admin/models/{id}/images", model.getId())
				.file(new MockMultipartFile("image", "m.png", "image/png", png(1024))).header("Authorization", tokenOf("demo@hansung.ac.kr", "STUDENT")))
			.andExpect(status().isForbidden());
	}

	@Test
	void B1_08_7_삭제가_실패해도_키가_DB에_남고_재시작_뒤_재시도로_지워진다() throws Exception {
		storage.local.put("profile/old.png", png(100), "image/png");
		storage.failDelete = true;
		deletionService.request("profile/old.png");
		assertThat(jobs.findAll()).singleElement().satisfies(j -> {
			assertThat(j.getObjectKey()).isEqualTo("profile/old.png");
			assertThat(j.getAttemptCount()).isEqualTo(1);
		});
		assertThat(storage.exists("profile/old.png")).isTrue();

		// 재시작: 저장소가 살아나고 남은 작업을 다시 읽어 시도
		storage.failDelete = false;
		deletionService.request("profile/already-gone.png");
		deletionService.retryAll();
		assertThat(jobs.count()).isZero();
		assertThat(storage.exists("profile/old.png")).isFalse();
	}

	@Test
	void B1_08_local_임시_주소는_10분_뒤와_서명이_틀리면_열리지_않는다() throws Exception {
		storage.local.put("profile/u.png", png(64), "image/png");
		String url = storage.temporaryUrl("profile/u.png");
		String path = url.substring(url.indexOf("/api/v1/files/local/"));
		mvc.perform(get(path)).andExpect(status().isOk());
		mvc.perform(get(path.replaceAll("signature=[0-9a-f]+", "signature=00"))).andExpect(status().isNotFound());
		clock.set(NOW.plusSeconds(601));
		mvc.perform(get(path)).andExpect(status().isNotFound());
	}
}
