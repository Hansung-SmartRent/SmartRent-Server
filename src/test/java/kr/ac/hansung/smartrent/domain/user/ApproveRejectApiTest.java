package kr.ac.hansung.smartrent.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.audit.AuditLog;
import kr.ac.hansung.smartrent.global.audit.AuditLogRepository;
import kr.ac.hansung.smartrent.global.port.AccountAlert;
import kr.ac.hansung.smartrent.global.port.OperationsAlert;
import kr.ac.hansung.smartrent.global.port.RetainedWarningRestorer;
import kr.ac.hansung.smartrent.global.storage.StorageDeletionJob;
import kr.ac.hansung.smartrent.global.storage.StorageDeletionJobRepository;
import kr.ac.hansung.smartrent.global.storage.StorageDeletionService;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * B1-15 서버 구현 후 확인할 것 1~5행. 저장소는 시험용 임시 폴더(FlakyStorage). 알림(B2-13 전)과 보관 경고 복원(B2-17 전)은
 * 호출을 기록하는 가짜로 확인합니다. 바꾼 계정은 끝에 처음 값으로 되돌립니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, ApproveRejectApiTest.Fakes.class})
class ApproveRejectApiTest {

	static final Instant NOW = OffsetDateTime.parse("2026-10-07T10:00:00+09:00").toInstant();
	static final List<String> TOUCHED = List.of("pending@hansung.ac.kr", "yerin@hansung.ac.kr");

	static class Recorder implements AccountAlert, OperationsAlert, RetainedWarningRestorer {
		final List<String> studentAlerts = new ArrayList<>();
		final List<String> operations = new ArrayList<>();
		final List<String> restored = new ArrayList<>();

		@Override
		public void toAdmins(String title, String body, Long relatedUserId) {
		}

		@Override
		public void toStudent(Long userId, String title, String body) {
			studentAlerts.add(userId + ":" + title);
		}

		@Override
		public void send(String title, String body) {
			operations.add(title);
		}

		@Override
		public void restore(Long userId, String studentNumber) {
			restored.add(userId + ":" + studentNumber);
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
		Recorder recorder() {
			return new Recorder();
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	FlakyStorage storage;
	@Autowired
	Recorder recorder;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	AuditLogRepository audits;
	@Autowired
	StorageDeletionJobRepository jobs;
	@Autowired
	StorageDeletionService deletionService;
	@Autowired
	JdbcTemplate jdbc;

	final List<Map<String, Object>> saved = new ArrayList<>();

	@BeforeEach
	void snapshot() {
		storage.failPut = false;
		storage.failDelete = false;
		recorder.studentAlerts.clear();
		recorder.operations.clear();
		recorder.restored.clear();
		jobs.deleteAll();
		saved.clear();
		for (String email : TOUCHED) {
			saved.add(jdbc.queryForMap("SELECT email, approval, reject_reason, student_id_image_key FROM users WHERE email = ?", email));
		}
	}

	@AfterEach
	void restore() {
		for (Map<String, Object> row : saved) {
			jdbc.update("UPDATE users SET approval = ?, reject_reason = ?, student_id_image_key = ? WHERE email = ?",
				row.get("approval"), row.get("reject_reason"), row.get("student_id_image_key"), row.get("email"));
		}
	}

	private User user(String email) {
		return users.findByEmail(email).orElseThrow();
	}

	/** 승인 대기 상태로 두고 저장소에 학생증 사진을 넣음 */
	private String pendingWithPhoto(String email) {
		String key = "student-id/b1-15-" + email.substring(0, email.indexOf('@')) + ".png";
		storage.put(key, new byte[] {1, 2, 3}, "image/png");
		jdbc.update("UPDATE users SET approval = 'PENDING', reject_reason = NULL, student_id_image_key = ? WHERE email = ?", key, email);
		return key;
	}

	private ResultActions admin(String path, String body) throws Exception {
		var req = post(path).header("Authorization",
			TestTokens.bearer(jwtEncoder, user("admin@hansung.ac.kr").getId(), "ADMIN", NOW.plusSeconds(1800)));
		if (body != null) {
			req.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mvc.perform(req);
	}

	private AuditLog lastAudit() {
		return audits.findAll().stream().max(java.util.Comparator.comparing(AuditLog::getId)).orElseThrow();
	}

	@Test
	void B1_15_1_이새봄을_승인하면_사진을_지우고_키를_비우고_알림과_관리_기록을_남긴다() throws Exception {
		User pending = user("pending@hansung.ac.kr");
		String key = pendingWithPhoto("pending@hansung.ac.kr");
		long before = audits.count();
		admin("/api/v1/admin/students/" + pending.getId() + "/approve", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.approval").value("APPROVED"))
			.andExpect(jsonPath("$.data.hasStudentIdImage").value(false));
		User after = user("pending@hansung.ac.kr");
		assertThat(after.getApproval()).isEqualTo(Approval.APPROVED);
		assertThat(after.getStudentIdImageKey()).isNull();
		assertThat(storage.exists(key)).isFalse();
		assertThat(jobs.count()).isZero();
		assertThat(recorder.studentAlerts).containsExactly(pending.getId() + ":계정 승인");
		assertThat(audits.count()).isEqualTo(before + 1);
		assertThat(lastAudit().getAction()).isEqualTo("APPROVE_STUDENT");
	}

	@Test
	void B1_15_2_사유와_함께_반려하면_사유를_저장하고_사진을_지운다() throws Exception {
		User yerin = user("yerin@hansung.ac.kr");
		String key = pendingWithPhoto("yerin@hansung.ac.kr");
		admin("/api/v1/admin/students/" + yerin.getId() + "/reject", "{\"reason\":\"사진이 흐립니다\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.approval").value("REJECTED"));
		User after = user("yerin@hansung.ac.kr");
		assertThat(after.getApproval()).isEqualTo(Approval.REJECTED);
		assertThat(after.getRejectReason()).isEqualTo("사진이 흐립니다");
		assertThat(after.getStudentIdImageKey()).isNull();
		assertThat(storage.exists(key)).isFalse();
		assertThat(recorder.studentAlerts).containsExactly(yerin.getId() + ":학생증 재제출 필요");
		assertThat(lastAudit().getAction()).isEqualTo("REJECT_STUDENT");
		assertThat(recorder.restored).isEmpty();
	}

	@Test
	void B1_15_3_이미_승인한_학생을_다시_승인하면_409() throws Exception {
		admin("/api/v1/admin/students/" + user("demo@hansung.ac.kr").getId() + "/approve", null)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("APPROVAL_NOT_PENDING"));
		admin("/api/v1/admin/students/" + user("demo@hansung.ac.kr").getId() + "/reject", "{\"reason\":\"다시\"}")
			.andExpect(status().isConflict());
	}

	@Test
	void B1_15_4_사진_삭제가_실패해도_승인은_유지되고_삭제_작업이_남았다가_다시_시도로_지워진다() throws Exception {
		User pending = user("pending@hansung.ac.kr");
		String key = pendingWithPhoto("pending@hansung.ac.kr");
		storage.failDelete = true;
		admin("/api/v1/admin/students/" + pending.getId() + "/approve", null).andExpect(status().isOk());
		assertThat(user("pending@hansung.ac.kr").getApproval()).isEqualTo(Approval.APPROVED);
		assertThat(user("pending@hansung.ac.kr").getStudentIdImageKey()).isNull();
		assertThat(jobs.findAll()).extracting(StorageDeletionJob::getObjectKey).containsExactly(key);
		assertThat(recorder.operations).containsExactly("사진 삭제 실패");
		// 저장소가 돌아오고 다시 시도하면(서버를 켤 때·1분마다) 파일과 작업이 모두 사라짐
		storage.failDelete = false;
		deletionService.retryAll();
		assertThat(storage.exists(key)).isFalse();
		assertThat(jobs.count()).isZero();
	}

	@Test
	void B1_15_5_승인하면_같은_학번의_보관_경고_복원을_부른다() throws Exception {
		User pending = user("pending@hansung.ac.kr");
		pendingWithPhoto("pending@hansung.ac.kr");
		admin("/api/v1/admin/students/" + pending.getId() + "/approve", null).andExpect(status().isOk());
		assertThat(recorder.restored).containsExactly(pending.getId() + ":" + pending.getStudentNumber());
	}

	@Test
	void B1_15_반려_사유가_비면_400이고_학생_토큰은_403() throws Exception {
		User pending = user("pending@hansung.ac.kr");
		pendingWithPhoto("pending@hansung.ac.kr");
		admin("/api/v1/admin/students/" + pending.getId() + "/reject", "{\"reason\":\" \"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(post("/api/v1/admin/students/" + pending.getId() + "/approve")
				.header("Authorization", TestTokens.bearer(jwtEncoder, user("demo@hansung.ac.kr").getId(), "STUDENT", NOW.plusSeconds(1800))))
			.andExpect(status().isForbidden());
		assertThat(user("pending@hansung.ac.kr").getApproval()).isEqualTo(Approval.PENDING);
	}
}
