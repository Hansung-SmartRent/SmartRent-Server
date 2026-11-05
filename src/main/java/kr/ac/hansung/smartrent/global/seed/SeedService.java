package kr.ac.hansung.smartrent.global.seed;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.ExampleFor;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelSource;
import kr.ac.hansung.smartrent.domain.equipment.entity.RentalType;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.operation.entity.Holiday;
import kr.ac.hansung.smartrent.domain.operation.entity.HolidaySource;
import kr.ac.hansung.smartrent.domain.operation.repository.HolidayRepository;
import kr.ac.hansung.smartrent.domain.rental.entity.ApplicationSource;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalApplication;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.entity.ReturnCondition;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalApplicationRepository;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalRepository;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 처음 데이터 넣기(B1-03). 표가 비어 있을 때만 넣어서 서버를 다시 켜도 늘지 않습니다.
 * 대여 예시(rentals.json)는 local에서만 넣고, 노쇼 예시의 경고는 경고 표(B2-12)가 생긴 뒤에 추가합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {
 private final kr.ac.hansung.smartrent.domain.warning.repository.WarningRepository warningRepository;

	private final EquipmentModelRepository modelRepository;
	private final EquipmentUnitRepository unitRepository;
	private final UserRepository userRepository;
	private final HolidayRepository holidayRepository;
	private final RentalApplicationRepository applicationRepository;
	private final RentalRepository rentalRepository;
	private final PasswordEncoder passwordEncoder;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	/** local 프로필: 기자재·가짜 계정·공휴일 */
	@Transactional
	public void loadLocal() {
		loadModels(read("equipment.json", SeedFixtures.Equipment.class).models());
		if (userRepository.count() == 0) {
			read("users.json", SeedFixtures.Users.class).users().forEach(this::saveUser);
			log.info("처음 데이터: 계정 {}개", userRepository.count());
		}
		if (holidayRepository.count() == 0) {
			read("holidays.json", SeedFixtures.Holidays.class).holidays().forEach(this::saveHoliday);
			log.info("처음 데이터: 공휴일 {}일", holidayRepository.count());
		}
		if (rentalRepository.count() == 0) {
			loadRentals(read("rentals.json", SeedFixtures.Rentals.class).rentals());
		}
  if(warningRepository.count()==0) {
   for(var fixture:read("users.json",SeedFixtures.Users.class).users()) {
    if(fixture.warnings()==null) continue;
    userRepository.findByEmail(fixture.email()).ifPresent(user->fixture.warnings().forEach(kind->
     warningRepository.save(kr.ac.hansung.smartrent.domain.warning.entity.Warning.builder()
      .userId(user.getId()).kind(kr.ac.hansung.smartrent.domain.warning.entity.WarningKind.valueOf(kind))
      .createdAt(LocalDateTime.now(clock)).build())));
   }
  }
	}

	/** 상태별 대여 예시(local만). 학생은 users.json의 key, 모델은 key, 기기는 code로 찾습니다 */
	private void loadRentals(List<SeedFixtures.Rental> rentals) {
		Map<String, String> emailByKey = read("users.json", SeedFixtures.Users.class).users().stream()
			.collect(Collectors.toMap(SeedFixtures.User::key, SeedFixtures.User::email));
		Map<String, EquipmentModel> modelByKey = modelRepository.findAll().stream()
			.collect(Collectors.toMap(EquipmentModel::getModelKey, Function.identity()));
		Map<String, EquipmentUnit> unitByCode = unitRepository.findAll().stream()
			.collect(Collectors.toMap(EquipmentUnit::getCode, Function.identity()));
		Set<LocalDate> closedDays = holidayRepository.findAll().stream()
			.filter(h -> !h.isOpenOverride()).map(Holiday::getDate).collect(Collectors.toSet());
		LocalDate today = operatingDay(LocalDate.now(clock), 1, closedDays);
		for (SeedFixtures.Rental r : rentals) {
			User student = userRepository.findByEmail(emailByKey.get(r.student()))
				.orElseThrow(() -> new IllegalStateException("대여 예시의 학생을 찾지 못했습니다: " + r.student()));
			EquipmentModel model = require(modelByKey.get(r.modelKey()), "모델", r.modelKey());
			EquipmentUnit unit = r.unitCode() == null ? null : require(unitByCode.get(r.unitCode()), "기기", r.unitCode());
			LocalDate day = shift(today, r.dayOffset(), closedDays);
			LocalDateTime start = day.atTime(LocalTime.parse(r.start()));
			boolean broken = Boolean.TRUE.equals(r.brokenReported());
			RentalStatus status = RentalStatus.valueOf(r.status());
			boolean picked = status == RentalStatus.RENTING || status == RentalStatus.RETURNED;
			RentalApplication application = applicationRepository.save(RentalApplication.builder()
				.userId(student.getId()).modelId(model.getId()).source(ApplicationSource.RESERVATION).build());
			rentalRepository.save(Rental.builder()
				.applicationId(application.getId())
				.userId(student.getId())
				.modelId(model.getId())
				.unitId(unit == null ? null : unit.getId())
				.status(status)
				.startAt(start)
				.endAt(day.atTime(LocalTime.parse(r.end())))
				.pickedAt(picked ? start : null)
				.returnedAt(r.returnedAt() == null ? null : day.atTime(LocalTime.parse(r.returnedAt())))
				.returnCondition(r.returnCondition() == null ? null : ReturnCondition.valueOf(r.returnCondition()))
				.brokenReported(broken)
				.brokenNote(r.brokenNote())
				.cancelReason(r.cancelReason())
				.build());
			if (broken && unit != null) {
				unit.changeCondition(UnitCondition.INSPECTION); // 신고한 기기는 반납 확인 전까지 내주지 않음(DB 설계 4절)
			}
		}
		log.info("처음 데이터: 대여 예시 {}건", rentalRepository.count());
	}

	private static <T> T require(T value, String what, String key) {
		if (value == null) {
			throw new IllegalStateException("대여 예시의 " + what + "를 찾지 못했습니다: " + key);
		}
		return value;
	}

	/** 운영일 기준으로 n일 옮깁니다. 0이면 그대로, 양수는 뒤로, 음수는 앞으로 */
	static LocalDate shift(LocalDate operatingDay, int n, Set<LocalDate> closedDays) {
		LocalDate day = operatingDay;
		for (int i = 0; i < Math.abs(n); i++) {
			day = operatingDay(day.plusDays(Integer.signum(n)), Integer.signum(n), closedDays);
		}
		return day;
	}

	/** 쉬는 날(토·일, 운영 해제하지 않은 공휴일)이면 step 방향으로 다음 운영일을 찾습니다 */
	static LocalDate operatingDay(LocalDate day, int step, Set<LocalDate> closedDays) {
		while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY || closedDays.contains(day)) {
			day = day.plusDays(step);
		}
		return day;
	}

	/** prod 프로필(SEED_PROD_ENABLED=true): 기자재와 관리자 계정만. 가짜 학생·대여·공휴일은 넣지 않음 */
	@Transactional
	public void loadProd(String adminEmail, String adminPassword) {
		if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.length() < 8) {
			throw new IllegalStateException("prod 처음 데이터를 넣으려면 ADMIN_EMAIL과 8자 이상 ADMIN_PASSWORD가 필요합니다.");
		}
		loadModels(read("equipment.json", SeedFixtures.Equipment.class).models());
		if (userRepository.findAll().stream().noneMatch(u -> u.getRole() == Role.ADMIN)) {
			userRepository.save(User.builder()
				.role(Role.ADMIN)
				.email(adminEmail.trim().toLowerCase(Locale.ROOT))
				.passwordHash(passwordEncoder.encode(adminPassword))
				.name("대여 관리자")
				.approval(Approval.APPROVED)
				.build());
			log.info("처음 데이터: 관리자 계정 1개");
		}
	}

	/** 모델·기기. 넣기 전에 운영 중인 모델끼리 이름(name_key)이 겹치는지 먼저 보고, 겹치면 하나도 넣지 않고 멈춤 */
	@Transactional
	public void loadModels(List<SeedFixtures.Model> models) {
		if (modelRepository.count() > 0) {
			return;
		}
		checkNameKeys(models);
		for (SeedFixtures.Model m : models) {
			EquipmentModel saved = modelRepository.save(EquipmentModel.builder()
				.source(ModelSource.valueOf(m.source()))
				.exampleFor(m.exampleFor() == null ? null : ExampleFor.valueOf(m.exampleFor()))
				.modelKey(m.key())
				.siteNumber(m.siteNumber())
				.category(m.category())
				.name(m.name())
				.nameKey(EquipmentModel.nameKeyOf(m.name()))
				.manufacturer(m.manufacturer())
				.purchaseYear(m.purchaseYear())
				.purpose(m.purpose())
				.rentalType(RentalType.valueOf(m.rentalType()))
				.multiDay(m.multiDay())
				.maxRentalDays(m.maxRentalDays())
				.maxRentalMinutes(m.maxRentalMinutes())
				.carryOut(m.carryOut())
				.location(m.location() == null ? "" : m.location())
				.contact(m.contact())
				.openTime(LocalTime.parse(m.openTime()))
				.closeTime(LocalTime.parse(m.closeTime()))
				.intervalMinutes(m.intervalMinutes())
				.guide(m.guide())
				.archived(false)
				.build());
			for (SeedFixtures.Unit u : m.units()) {
				unitRepository.save(EquipmentUnit.builder()
					.modelId(saved.getId())
					.code(u.code())
					.name(u.name())
					.condition(UnitCondition.valueOf(u.condition()))
					.build());
			}
		}
		log.info("처음 데이터: 모델 {}개, 기기 {}대", modelRepository.count(), unitRepository.count());
	}

	static void checkNameKeys(List<SeedFixtures.Model> models) {
		Map<String, List<String>> byKey = new LinkedHashMap<>();
		for (SeedFixtures.Model m : models) {
			byKey.computeIfAbsent(EquipmentModel.nameKeyOf(m.name()), k -> new ArrayList<>()).add(m.name());
		}
		List<String> dup = byKey.values().stream().filter(v -> v.size() > 1).map(v -> String.join(" / ", v)).toList();
		if (!dup.isEmpty()) {
			throw new IllegalStateException("운영 중인 모델 이름이 겹칩니다(대소문자·공백 무시): " + String.join(", ", dup));
		}
	}

	private void saveUser(SeedFixtures.User u) {
		Integer months = u.suspendedUntilMonthsFromLoad();
		userRepository.save(User.builder()
			.role(Role.valueOf(u.role()))
			.email(u.email())
			.passwordHash(passwordEncoder.encode(u.password()))
			.name(u.name())
			.studentNumber(u.studentNumber())
			.approval(Approval.valueOf(u.approval()))
			.rejectReason(u.rejectReason())
			.suspendedUntil(months == null ? null : LocalDateTime.now(clock).plusMonths(months))
			.build());
	}

	private void saveHoliday(SeedFixtures.Holiday h) {
		holidayRepository.save(Holiday.builder()
			.date(LocalDate.parse(h.date()))
			.name(h.name())
			.source(HolidaySource.valueOf(h.source()))
			.openOverride(h.openOverride())
			.build());
	}

	private <T> T read(String file, Class<T> type) {
		try (InputStream in = new ClassPathResource("fixtures/" + file).getInputStream()) {
			return jsonMapper.readValue(in, type);
		}
		catch (IOException e) {
			throw new IllegalStateException("처음 데이터 파일을 읽지 못했습니다: fixtures/" + file, e);
		}
	}
}
