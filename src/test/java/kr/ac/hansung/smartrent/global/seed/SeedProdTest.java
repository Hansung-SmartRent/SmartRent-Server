package kr.ac.hansung.smartrent.global.seed;

import static org.assertj.core.api.Assertions.assertThat;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.operation.repository.HolidayRepository;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** B1-03 서버 구현 후 확인할 것 3행: prod 프로필은 기자재와 관리자만 넣는다 */
@SpringBootTest(properties = {"smartrent.seed.prod-enabled=true", "smartrent.seed.admin-email=Ops@Hansung.ac.kr",
	"smartrent.seed.admin-password=test-only-password"})
@ActiveProfiles("prod")
@Import(TestcontainersConfiguration.class)
class SeedProdTest {

	@Autowired
	EquipmentModelRepository models;
	@Autowired
	UserRepository users;
	@Autowired
	HolidayRepository holidays;

	@Test
	void B1_03_3_prod는_학생_0명_기자재와_관리자만_들어간다() {
		assertThat(models.count()).isEqualTo(43);
		assertThat(users.findAll()).hasSize(1).allSatisfy(u -> {
			assertThat(u.getRole()).isEqualTo(Role.ADMIN);
			assertThat(u.getEmail()).isEqualTo("ops@hansung.ac.kr");
			assertThat(u.getPasswordHash()).startsWith("$2");
		});
		assertThat(holidays.count()).isZero();
	}
}
