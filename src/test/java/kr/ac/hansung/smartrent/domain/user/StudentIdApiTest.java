package kr.ac.hansung.smartrent.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.port.AccountAlert;
import kr.ac.hansung.smartrent.support.FlakyStorage;
import kr.ac.hansung.smartrent.support.MutableClock;
import kr.ac.hansung.smartrent.support.TestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * B1-07 서버 구현 후 확인할 것 1~5행. 저장소는 시험용 임시 폴더(FlakyStorage), 관리자 알림은 기록만 하는 가짜(B2-13 전).
 * 시험끼리 계정이 섞이지 않게 바꾼 계정은 끝에 처음 값으로 되돌립니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, StudentIdApiTest.Fakes.class})
class StudentIdApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();
	static final List<String> TOUCHED = List.of("newstudent@hansung.ac.kr", "yerin@hansung.ac.kr");

	static class RecordingAlert implements AccountAlert {
		final List<Long> sent = new ArrayList<>();

		@Override
		public void toAdmins(String title, String body, Long relatedUserId) {
			sent.add(relatedUserId);
		}
	}

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

		@Bean
		@Primary
		RecordingAlert recordingAlert() {
			return new RecordingAlert();
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	FlakyStorage storage;
	@Autowired
	RecordingAlert alert;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	JdbcTemplate jdbc;

	final List<Map<String, Object>> saved = new ArrayList<>();

	@BeforeEach
	void snapshot() {
		storage.failPut = false;
		alert.sent.clear();
		saved.clear();
		for (String email : TOUCHED) {
			saved.add(jdbc.queryForMap("SELECT email, name, student_number, approval, reject_reason, student_id_image_key FROM users WHERE email = ?", email));
		}
	}

	@AfterEach
	void restore() {
		for (Map<String, Object> row : saved) {
			jdbc.update("UPDATE users SET name = ?, student_number = ?, approval = ?, reject_reason = ?, student_id_image_key = ? WHERE email = ?",
				row.get("name"), row.get("student_number"), row.get("approval"), row.get("reject_reason"),
				row.get("student_id_image_key"), row.get("email"));
		}
	}

	static byte[] jpg() {
		byte[] b = new byte[2048];
		b[0] = (byte) 0xFF;
		b[1] = (byte) 0xD8;
		b[2] = (byte) 0xFF;
		Arrays.fill(b, 3, b.length, (byte) 5);
		return b;
	}

	private User user(String email) {
		return users.findByEmail(email).orElseThrow();
	}

	private ResultActions submit(String email, String role, String name, String number, MockMultipartFile image) throws Exception {
		var req = multipart("/api/v1/users/me/student-id").param("name", name).param("studentNumber", number);
		if (image != null) {
			req.file(image);
		}
		return mvc.perform(req.header("Authorization", TestTokens.bearer(jwtEncoder, user(email).getId(), role, NOW.plusSeconds(1800))));
	}

	private static MockMultipartFile image(String filename, String type, byte[] content) {
		return new MockMultipartFile("image", filename, type, content);
	}

	@Test
	void B1_07_1_미제출_학생이_내면_PENDING이고_사진이_student_id에_있고_관리자에게_알린다() throws Exception {
		submit("newstudent@hansung.ac.kr", "STUDENT", "새학생", "2499100", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.approval").value("PENDING"))
			.andExpect(jsonPath("$.data.name").value("새학생"))
			.andExpect(jsonPath("$.data.studentNumber").value("2499100"));
		User u = user("newstudent@hansung.ac.kr");
		assertThat(u.getApproval()).isEqualTo(Approval.PENDING);
		assertThat(u.getStudentIdImageKey()).startsWith("student-id/").doesNotContain("2499100").doesNotContain("새학생");
		assertThat(storage.exists(u.getStudentIdImageKey())).isTrue();
		assertThat(alert.sent).containsExactly(u.getId());
	}

	@Test
	void B1_07_2_다른_학생이_쓰는_학번은_409() throws Exception {
		submit("newstudent@hansung.ac.kr", "STUDENT", "새학생", "2499005", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("STUDENT_NUMBER_DUPLICATE"));
		assertThat(user("newstudent@hansung.ac.kr").getApproval()).isEqualTo(Approval.NONE);
		assertThat(alert.sent).isEmpty();
	}

	@Test
	void B1_07_3_반려된_학생이_다시_내면_PENDING이고_반려_사유가_비워진다() throws Exception {
		assertThat(user("yerin@hansung.ac.kr").getRejectReason()).isNotBlank();
		submit("yerin@hansung.ac.kr", "STUDENT", "강예린", "2499009", image("id.png", "image/png", PhotoBytes.png()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.approval").value("PENDING"));
		User u = user("yerin@hansung.ac.kr");
		assertThat(u.getApproval()).isEqualTo(Approval.PENDING);
		assertThat(u.getRejectReason()).isNull();
		assertThat(storage.exists(u.getStudentIdImageKey())).isTrue();
	}

	@Test
	void B1_07_4_gif는_400_FILE_INVALID() throws Exception {
		submit("newstudent@hansung.ac.kr", "STUDENT", "새학생", "2499100", image("id.gif", "image/gif", "GIF89a.......".getBytes()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("FILE_INVALID"));
		assertThat(user("newstudent@hansung.ac.kr").getApproval()).isEqualTo(Approval.NONE);
	}

	@Test
	void B1_07_5_이미_승인된_학생은_409_APPROVAL_NOT_PENDING() throws Exception {
		submit("demo@hansung.ac.kr", "STUDENT", "김하늘", "2499001", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("APPROVAL_NOT_PENDING"));
	}

	@Test
	void B1_07_승인_대기_중에는_다시_내지_못한다() throws Exception {
		submit("pending@hansung.ac.kr", "STUDENT", "이새봄", "2499002", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("APPROVAL_NOT_PENDING"));
	}

	@Test
	void B1_07_학번_형식이_틀리거나_사진이_없으면_400() throws Exception {
		submit("newstudent@hansung.ac.kr", "STUDENT", "새학생", "24-99100", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		submit("newstudent@hansung.ac.kr", "STUDENT", "새학생", "2499100", null)
			.andExpect(status().isBadRequest());
	}

	@Test
	void B1_07_저장소가_실패하면_503이고_계정은_그대로() throws Exception {
		storage.failPut = true;
		submit("newstudent@hansung.ac.kr", "STUDENT", "새학생", "2499100", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.errorCode").value("STORAGE_UNAVAILABLE"));
		User u = user("newstudent@hansung.ac.kr");
		assertThat(u.getApproval()).isEqualTo(Approval.NONE);
		assertThat(u.getStudentIdImageKey()).isNull();
		assertThat(alert.sent).isEmpty();
	}

	@Test
	void B1_07_관리자는_낼_수_없다() throws Exception {
		submit("admin@hansung.ac.kr", "ADMIN", "관리자", "2499999", image("id.jpg", "image/jpeg", jpg()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
	}

	/** png 머리글이 붙은 시험용 사진 */
	static final class PhotoBytes {
		static byte[] png() {
			byte[] b = new byte[2048];
			byte[] head = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
			System.arraycopy(head, 0, b, 0, head.length);
			Arrays.fill(b, head.length, b.length, (byte) 7);
			return b;
		}
	}
}
