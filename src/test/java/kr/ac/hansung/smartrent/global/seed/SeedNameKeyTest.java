package kr.ac.hansung.smartrent.global.seed;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/** B1-03 서버 구현 후 확인할 것 5행: 운영 중 모델 이름이 대소문자만 다르게 겹치면 넣지 않고 멈춘다 */
class SeedNameKeyTest {

	@Test
	void B1_03_5_이름만_대소문자가_다른_모델이_있으면_겹친_이름을_알리고_멈춘다() {
		List<SeedFixtures.Model> models = List.of(model("1", "Insta360 EVO"), model("99", "insta360  evo"));

		assertThatThrownBy(() -> SeedService.checkNameKeys(models))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Insta360 EVO / insta360  evo");
	}

	private static SeedFixtures.Model model(String key, String name) {
		return new SeedFixtures.Model(key, "SITE", null, key, "VR/AR/기타", name, null, null, null, "GENERAL", false,
			null, null, true, "상상관", null, "09:00", "18:00", 60, null, List.of());
	}
}
