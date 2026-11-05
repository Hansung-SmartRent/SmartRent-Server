package kr.ac.hansung.smartrent.domain.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * B1-10 서버 구현 후 확인할 것 1~10행. local 처음 데이터 위에서, 지금은 2026-10-07(수) 10:05.
 * 처음 데이터의 대여 예시: 오늘 포터블 모니터(15) 1대 대여 중, ICLE-6400 카메라(13)의 기기 205는 파손 신고로 점검 중.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, ModelApiTest.Fakes.class})
class ModelApiTest {

	static final Instant WEDNESDAY = OffsetDateTime.parse("2026-10-07T10:05:00+09:00").toInstant();
	static final Instant SATURDAY = OffsetDateTime.parse("2026-10-10T11:00:00+09:00").toInstant();

	@TestConfiguration
	static class Fakes {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(WEDNESDAY);
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	MutableClock clock;
	@Autowired
	JwtEncoder jwtEncoder;
	@Autowired
	UserRepository users;
	@Autowired
	EquipmentModelRepository models;
	@Autowired
	EquipmentUnitRepository units;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	JsonMapper json;

	@BeforeEach
	void reset() {
		clock.set(WEDNESDAY);
	}

	private String demo() {
		return TestTokens.bearer(jwtEncoder, users.findByEmail("demo@hansung.ac.kr").orElseThrow().getId(), "STUDENT",
			clock.instant().plusSeconds(1800));
	}

	private ResultActions call(String url) throws Exception {
		return mvc.perform(get(url).header("Authorization", demo()));
	}

	private JsonNode data(String url) throws Exception {
		String body = call(url).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}

	private Map<String, EquipmentModel> byKey() {
		return models.findAll().stream().collect(Collectors.toMap(EquipmentModel::getModelKey, Function.identity()));
	}

	private long normalUnits(EquipmentModel m) {
		return units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(m.getId()).stream()
			.filter(u -> u.getCondition() == UnitCondition.NORMAL).count();
	}

	private JsonNode summaryOf(JsonNode list, EquipmentModel m) {
		for (JsonNode n : list) {
			if (n.get("id").asLong() == m.getId()) {
				return n;
			}
		}
		throw new AssertionError("목록에 없음: " + m.getName());
	}

	@Test
	void B1_10_1_목록은_43개이고_모델마다_source가_있다() throws Exception {
		JsonNode list = data("/api/v1/models");
		assertThat(list).hasSize(43);
		for (JsonNode n : list) {
			assertThat(n.get("source").asString()).isIn("SITE", "EXAMPLE", "ADMIN");
		}
	}

	@Test
	void B1_10_1_로그인하지_않으면_401() throws Exception {
		mvc.perform(get("/api/v1/models")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void B1_10_2_이름_일부로_찾으면_대소문자_무시하고_나온다() throws Exception {
		JsonNode list = data("/api/v1/models?q=insta");
		assertThat(list).extracting(n -> n.get("name").asString()).contains("Insta360 EVO");
		assertThat(data("/api/v1/models?q=zzz-없는-이름")).isEmpty();
	}

	@Test
	void B1_10_3_카테고리로_거르면_그_카테고리만_나온다() throws Exception {
		JsonNode list = data("/api/v1/models?category=3D 프린터");
		assertThat(list).isNotEmpty();
		assertThat(list).extracting(n -> n.get("category").asString()).containsOnly("3D 프린터");
	}

	@Test
	void B1_10_4_운영_종료한_모델은_목록과_상세에서_빠진다() throws Exception {
		EquipmentModel oculus = byKey().get("7");
		jdbc.update("UPDATE equipment_models SET archived = TRUE, name_key = NULL WHERE id = ?", oculus.getId());
		try {
			JsonNode list = data("/api/v1/models");
			assertThat(list).hasSize(42);
			assertThat(list).extracting(n -> n.get("id").asLong()).doesNotContain(oculus.getId());
			call("/api/v1/models/" + oculus.getId()).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		}
		finally {
			jdbc.update("UPDATE equipment_models SET archived = FALSE, name_key = ? WHERE id = ?",
				EquipmentModel.nameKeyOf(oculus.getName()), oculus.getId());
		}
	}

	@Test
	void B1_10_5_평일_10시5분에는_점검_중_기기와_지금_대여_중인_기기를_뺀다() throws Exception {
		JsonNode list = data("/api/v1/models");
		Map<String, EquipmentModel> byKey = byKey();
		// 포터블 모니터: 정상 기기 중 1대가 오늘 대여 중
		EquipmentModel monitor = byKey.get("15");
		assertThat(summaryOf(list, monitor).get("normalUnits").asLong()).isEqualTo(normalUnits(monitor));
		assertThat(summaryOf(list, monitor).get("availableNow").asLong()).isEqualTo(normalUnits(monitor) - 1);
		// 카메라: 파손 신고한 기기는 점검 중이라 정상 기기 수와 지금 대수 모두에서 빠짐
		EquipmentModel camera = byKey.get("13");
		long cameraAll = units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(camera.getId()).size();
		assertThat(summaryOf(list, camera).get("availableNow").asLong()).isEqualTo(normalUnits(camera)).isLessThan(cameraAll);
		// 학교 사이트에서 대여 가능 0이던 점검 중 기기 7대도 모두 빠짐
		long inspection = units.findAll().stream().filter(u -> u.getCondition() == UnitCondition.INSPECTION).count();
		long totalUnits = units.count();
		long sumNormal = 0;
		for (JsonNode n : list) {
			sumNormal += n.get("normalUnits").asLong();
			assertThat(n.get("availableNow").asLong()).isLessThanOrEqualTo(n.get("normalUnits").asLong());
		}
		assertThat(inspection).isEqualTo(8); // 사이트의 7대 + 파손 신고 예시 1대
		assertThat(sumNormal).isEqualTo(totalUnits - inspection);
	}

	@Test
	void B1_10_5_운영_시작_전_모델은_0() throws Exception {
		// Oculus Quest(8)는 10:00~15:00 운영. 09:30에는 운영 전이라 0
		clock.set(OffsetDateTime.parse("2026-10-07T09:30:00+09:00").toInstant());
		EquipmentModel quest = byKey().get("8");
		assertThat(summaryOf(data("/api/v1/models"), quest).get("availableNow").asLong()).isZero();
	}

	@Test
	void B1_10_6_토요일에는_모든_모델이_0() throws Exception {
		clock.set(SATURDAY);
		JsonNode list = data("/api/v1/models");
		assertThat(list).hasSize(43);
		assertThat(list).extracting(n -> n.get("availableNow").asLong()).containsOnly(0L);
		assertThat(data("/api/v1/models/" + byKey().get("48").getId()).get("todayHours").isNull()).isTrue();
	}

	@Test
	void B1_10_7_학생용_상세에는_개별_기기_목록이_없다() throws Exception {
		EquipmentModel insta = byKey().get("48");
		JsonNode detail = data("/api/v1/models/" + insta.getId());
		assertThat(detail.has("units")).isFalse();
		assertThat(detail.get("name").asString()).isEqualTo("Insta360 EVO");
		assertThat(detail.get("todayHours").get("open").asString()).isEqualTo("09:00");
		assertThat(detail.get("todayHours").get("close").asString()).isEqualTo("20:00");
		assertThat(detail.get("normalUnits").asLong()).isEqualTo(normalUnits(insta));
		assertThat(detail.get("source").asString()).isEqualTo("SITE");
		assertThat(detail.get("imageUrls").isArray()).isTrue();
	}

	@Test
	void B1_10_7_없는_id는_404() throws Exception {
		call("/api/v1/models/999999").andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	@Test
	void B1_10_8_포터블_모니터는_multiDay와_maxRentalDays_7() throws Exception {
		JsonNode detail = data("/api/v1/models/" + byKey().get("15").getId());
		assertThat(detail.get("multiDay").asBoolean()).isTrue();
		assertThat(detail.get("maxRentalDays").asInt()).isEqualTo(7);
		assertThat(detail.get("maxRentalMinutes").isNull()).isTrue();
	}

	@Test
	void B1_10_9_레이저_커팅기는_maxRentalMinutes_120() throws Exception {
		JsonNode detail = data("/api/v1/models/" + byKey().get("29").getId());
		assertThat(detail.get("maxRentalMinutes").asInt()).isEqualTo(120);
		assertThat(detail.get("maxRentalDays").isNull()).isTrue();
	}

	@Test
	void B1_10_10_Oculus_rift_S는_carryOut_false() throws Exception {
		assertThat(data("/api/v1/models/" + byKey().get("7").getId()).get("carryOut").asBoolean()).isFalse();
		JsonNode summary = summaryOf(data("/api/v1/models"), byKey().get("7"));
		assertThat(summary.get("carryOut").asBoolean()).isFalse();
	}

	@Test
	void B1_10_예시_모델은_source_EXAMPLE로_목록에_나오고_장기만_거를_수_있다() throws Exception {
		JsonNode longTerm = data("/api/v1/models?rentalType=LONG_TERM");
		assertThat(longTerm).hasSize(5);
		assertThat(longTerm).extracting(n -> n.get("source").asString()).containsOnly("EXAMPLE");
		List<String> examples = List.of("demo-lg", "demo-asus", "demo-lenovo");
		JsonNode all = data("/api/v1/models");
		for (String key : examples) {
			assertThat(summaryOf(all, byKey().get(key)).get("source").asString()).isEqualTo("EXAMPLE");
		}
	}
}
