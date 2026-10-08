package kr.ac.hansung.smartrent.domain.equipment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** B1-02 서버 구현 후 확인할 것 3·4행: 운영 중인 모델끼리만 name_key가 겹치지 않는다 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EquipmentModelNameKeyTest {

	@Autowired
	EquipmentModelRepository repository;

	@AfterEach
	void clean() {
		repository.deleteAll();
	}

	@Test
	void B1_02_3_운영_중_모델_두_개에_같은_name_key면_두_번째_저장이_실패한다() {
		repository.saveAndFlush(model("t-1", "insta360evo"));

		assertThatThrownBy(() -> repository.saveAndFlush(model("t-2", "insta360evo")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void B1_02_4_운영_종료로_name_key가_NULL인_모델은_여러_개_저장된다() {
		repository.saveAndFlush(model("t-3", null));

		assertThatCode(() -> repository.saveAndFlush(model("t-4", null))).doesNotThrowAnyException();
	}

	private static EquipmentModel model(String key, String nameKey) {
		return EquipmentModel.builder()
			.source(ModelSource.ADMIN)
			.modelKey(key)
			.category("VR/AR/기타")
			.name("Insta360 EVO")
			.nameKey(nameKey)
			.rentalType(RentalType.GENERAL)
			.location("상상관 기자재실")
			.openTime(LocalTime.of(9, 0))
			.closeTime(LocalTime.of(18, 0))
			.intervalMinutes(60)
			.archived(nameKey == null)
			.build();
	}
}
