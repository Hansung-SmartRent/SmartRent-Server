package kr.ac.hansung.smartrent.global.seed;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** fixtures/*.json 모양. 필요한 칸만 읽고 나머지(about, note, appImageId 등)는 무시합니다. */
final class SeedFixtures {

	private SeedFixtures() {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Equipment(List<Model> models) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Model(String key, String source, String exampleFor, String siteNumber, String category, String name,
		String manufacturer, String purchaseYear, String purpose, String rentalType, boolean multiDay,
		Integer maxRentalDays, Integer maxRentalMinutes, boolean carryOut, String location, String contact,
		String openTime, String closeTime, int intervalMinutes, String guide, List<Unit> units) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Unit(String code, String name, String condition) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Users(List<User> users) {
	}

	/** suspendedUntilMonthsFromLoad: 넣는 시각 + N개월까지 정지. 경고(warnings)는 B2-12 표라 여기서 넣지 않음 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record User(String key, String role, String email, String password, String name, String studentNumber,
		String approval, String rejectReason, Integer suspendedUntilMonthsFromLoad, List<String> warnings) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Holidays(List<Holiday> holidays) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Rentals(List<Rental> rentals) {
	}

	/** dayOffset: 넣는 날 기준 운영일 N일 뒤(0은 넣는 날, 쉬는 날이면 다음 운영일). start·end·returnedAt은 그날의 시각 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Rental(String key, String student, String modelKey, String unitCode, String status, int dayOffset,
		String start, String end, String returnedAt, String returnCondition, Boolean brokenReported,
		String brokenNote, String cancelReason) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Holiday(String date, String name, String source, boolean openOverride) {
	}
}
