package kr.ac.hansung.smartrent.global.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.ExampleFor;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.operation.repository.HolidayRepository;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** B1-03 서버 구현 후 확인할 것 1·2·4·6행: local 프로필로 켜면 처음 데이터가 들어간다 */
@SpringBootTest
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class SeedLocalTest {

	@Autowired
	SeedService seedService;
	@Autowired
	EquipmentModelRepository models;
	@Autowired
	EquipmentUnitRepository units;
	@Autowired
	UserRepository users;
	@Autowired
	HolidayRepository holidays;

	@Test
	void B1_03_1_빈_DB로_켜면_모델_43_기기_153_계정_7_공휴일_40() {
		assertThat(models.count()).isEqualTo(43);
		assertThat(models.findAll().stream().filter(m -> m.getSource() == ModelSource.SITE).count()).isEqualTo(35);
		assertThat(models.findAll().stream().filter(m -> m.getSource() == ModelSource.EXAMPLE).count()).isEqualTo(8);
		assertThat(units.count()).isEqualTo(153);
		assertThat(users.count()).isEqualTo(7);
		assertThat(holidays.count()).isEqualTo(40);
	}

	@Test
	void B1_03_2_다시_켜도_행_수가_그대로() {
		seedService.loadLocal();

		assertThat(models.count()).isEqualTo(43);
		assertThat(units.count()).isEqualTo(153);
		assertThat(users.count()).isEqualTo(7);
		assertThat(holidays.count()).isEqualTo(40);
	}

	@Test
	void B1_03_4_비밀번호는_원문이_아니라_BCrypt로_저장된다() {
		for (User u : users.findAll()) {
			assertThat(u.getPasswordHash()).startsWith("$2").isNotEqualTo("demo1234");
		}
	}

	@Test
	void B1_03_6_학교_사이트_원문을_따른_설정이_그대로_들어간다() {
		Map<String, EquipmentModel> byKey = byKey();
		for (String key : new String[] {"5", "6", "7", "9", "12"}) {
			assertThat(byKey.get(key).isCarryOut()).as(key).isFalse();
		}
		assertThat(byKey.get("15").isMultiDay()).isTrue();
		assertThat(byKey.get("15").getMaxRentalDays()).isEqualTo(7);
		assertThat(byKey.get("29").getMaxRentalMinutes()).isEqualTo(120);
		byKey.forEach((key, m) -> {
			if (!key.equals("15")) {
				assertThat(m.getMaxRentalDays()).as(key).isNull();
			}
			if (!key.equals("29")) {
				assertThat(m.getMaxRentalMinutes()).as(key).isNull();
			}
		});
	}

	@Test
	void B1_03_예시_모델_8종이_example_for와_함께_들어가고_하루_대여로_바뀌지_않는다() {
		Map<String, EquipmentModel> byKey = byKey();
		for (String key : new String[] {"30", "31", "32", "33", "34"}) {
			EquipmentModel m = byKey.get(key);
			assertThat(m.getSource()).isEqualTo(ModelSource.EXAMPLE);
			assertThat(m.getExampleFor()).isEqualTo(ExampleFor.LONG_TERM);
			assertThat(m.getRentalType()).isEqualTo(RentalType.LONG_TERM);
		}
		for (String key : new String[] {"demo-lg", "demo-asus", "demo-lenovo"}) {
			EquipmentModel m = byKey.get(key);
			assertThat(m.getSource()).isEqualTo(ModelSource.EXAMPLE);
			assertThat(m.getExampleFor()).isEqualTo(ExampleFor.MULTI_DAY);
			assertThat(m.isMultiDay()).isTrue();
		}
	}

	@Test
	void B1_03_정지된_예시_학생은_넣는_시각부터_정지_기한이_있다() {
		User taeo = users.findAll().stream().filter(u -> "taeo@hansung.ac.kr".equals(u.getEmail())).findFirst().orElseThrow();
		assertThat(taeo.getSuspendedUntil()).isNotNull();
	}

	private Map<String, EquipmentModel> byKey() {
		return models.findAll().stream().collect(Collectors.toMap(EquipmentModel::getModelKey, Function.identity()));
	}
}
