package kr.ac.hansung.smartrent.domain.rental;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import jakarta.persistence.EntityManager;
import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import kr.ac.hansung.smartrent.domain.equipment.entity.*;
import kr.ac.hansung.smartrent.domain.equipment.repository.*;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.entity.*;
import kr.ac.hansung.smartrent.domain.rental.repository.*;
import kr.ac.hansung.smartrent.domain.rental.service.*;
import kr.ac.hansung.smartrent.global.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RentalPersistenceTest.FixedTime.class})
class RentalPersistenceTest {
    @org.springframework.boot.test.context.TestConfiguration
    static class FixedTime {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        Clock rentalTestClock() {
            return Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"),ZoneId.of("Asia/Seoul"));
        }
    }
    @Autowired RentalModelLockRepository locks;
    @Autowired RentalRepository rentals;
    @Autowired RentalApplicationRepository applications;
    @Autowired EquipmentModelRepository models;
    @Autowired EquipmentUnitRepository units;
    @Autowired RentalCapacityService capacity;
    @Autowired RentalCalendarReader calendars;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    Long modelId;
    Long userA;
    Long userB;
    TransactionTemplate tx;
    final LocalDateTime start=LocalDateTime.of(2026,10,7,10,0);

    @BeforeEach void setup() {
        tx=new TransactionTemplate(manager);
        tx.executeWithoutResult(s -> {
            var model=models.saveAndFlush(EquipmentModel.builder().source(ModelSource.ADMIN).modelKey("b2-01-test")
                .category("VR/AR/기타").name("Insta360 EVO").rentalType(RentalType.GENERAL).location("상상관 기자재실")
                .openTime(LocalTime.of(9,0)).closeTime(LocalTime.of(20,0)).intervalMinutes(60).build());
            modelId=model.getId();
            units.saveAndFlush(EquipmentUnit.builder().modelId(modelId).code("b2-01-unit").name("Insta360 EVO 1?").condition(UnitCondition.NORMAL).build());
            userA=insertUser("demo@hansung.ac.kr"); userB=insertUser("minjun@hansung.ac.kr");
        });
    }
    Long insertUser(String email) {
        jdbc.update("INSERT INTO users(role,email,password_hash,approval,suspended_until_return,suspension_started_by_return,created_at,updated_at) VALUES ('STUDENT',?,'test-only','APPROVED',false,false,?,?)",email,start,start);
        return jdbc.queryForObject("SELECT id FROM users WHERE email=?",Long.class,email);
    }
    @AfterEach void clean() {
        tx.executeWithoutResult(s -> {
            jdbc.update("DELETE FROM operating_hours WHERE model_id=?",modelId);
            jdbc.update("DELETE FROM holidays WHERE name='B2-01-test'");
            jdbc.update("DELETE FROM users WHERE id IN (?,?)",userA,userB);
            jdbc.update("DELETE FROM equipment_units WHERE model_id=?",modelId);
            jdbc.update("DELETE FROM equipment_models WHERE id=?",modelId);
        });
    }
    Rental create(Long user) {
        var app=applications.saveAndFlush(RentalApplication.builder().userId(user).modelId(modelId).source(ApplicationSource.RESERVATION).build());
        return rentals.saveAndFlush(Rental.builder().applicationId(app.getId()).userId(user).modelId(modelId)
            .status(RentalStatus.RESERVED).startAt(start).endAt(start.plusHours(1)).build());
    }
    @Test void B2_01_06_last_unit_two_transactions() throws Exception {
        var ready=new CountDownLatch(2); var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<String>> results=new ArrayList<>();
            for(Long user:List.of(userA,userB)) results.add(pool.submit(() -> {
                ready.countDown(); if(!go.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    return tx.execute(s -> {
                        var model=locks.lockById(modelId).orElseThrow();
                        if(capacity.available(model,new RentalSegment(start,start.plusHours(1)))==0) throw new BusinessException(ErrorCode.NO_CAPACITY);
                        create(user); return "SUCCESS";
                    });
                } catch(BusinessException e) { return e.getErrorCode().name(); }
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(List.of(results.get(0).get(20,TimeUnit.SECONDS),results.get(1).get(20,TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder("SUCCESS","NO_CAPACITY");
            assertThat(rentals.findAllByModelIdAndStatusIn(modelId,List.of(RentalStatus.RESERVED))).hasSize(1);
        } finally { go.countDown(); }
    }
    @Test void B2_01_schema_columns_match_design() throws Exception {
        String doc=java.nio.file.Files.readString(java.nio.file.Path.of("docs/db/README.md"));
        for(String table:List.of("rental_applications","rentals")) {
            String section=doc.split("### "+table+" ",2)[1].split("### ",2)[0];
            Map<String,String> expected=new TreeMap<>();
            for(String line:section.split("\n")) {
                String[] cells=line.split("\\|");
                if(cells.length<4 || !cells[2].trim().matches("[A-Z]+(\\(\\d+\\))?")) continue;
                String type=cells[2].trim().toLowerCase(Locale.ROOT);
                if(type.equals("boolean")) type="tinyint(1)";
                String required=cells[3].trim().startsWith("필수")?"NO":"YES";
                for(String name:cells[1].split(",")) expected.put(name.trim(),type+":"+required);
            }
            Map<String,String> actual=new TreeMap<>();
            jdbc.query("SELECT column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?",
                rs -> { actual.put(rs.getString(1),rs.getString(2)+":"+rs.getString(3)); },table);
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
        }
    }
    @Test void B2_01_version_conflict_and_schema() {
        Long id=tx.execute(s -> create(userA).getId());
        Rental first=rentals.findById(id).orElseThrow(); Rental stale=rentals.findById(id).orElseThrow();
        tx.executeWithoutResult(s -> {
            var managed=em.find(Rental.class,id);
            org.springframework.test.util.ReflectionTestUtils.setField(managed,"endAt",start.plusHours(2));
            em.flush();
            assertThat(managed.getVersion()).isEqualTo(first.getVersion()+1);
        });
        assertThatThrownBy(() -> rentals.saveAndFlush(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(jdbc.queryForMap("SHOW CREATE TABLE rentals").get("Create Table").toString())
            .contains("`version` bigint NOT NULL","`status` varchar(12) NOT NULL","`returned_at` datetime DEFAULT NULL");
        assertThat(first.getCreatedAt()).isNotNull();
    }
    @Test void B2_01_calendar_date_default_model_priority_and_holiday_override() {
        tx.executeWithoutResult(s -> {
            var model=models.findById(modelId).orElseThrow();
            assertThat(calendars.read(model).hours(start.toLocalDate()).close()).isEqualTo(LocalTime.of(20,0));
            jdbc.update("INSERT INTO operating_hours(model_id,date,open_time,close_time) VALUES (?,NULL,'10:00','18:00')",modelId);
            jdbc.update("INSERT INTO operating_hours(model_id,date,open_time,close_time) VALUES (?,'2026-10-07','11:00','16:00')",modelId);
            var calendar=calendars.read(model);
            assertThat(calendar.hours(start.toLocalDate()).close()).isEqualTo(LocalTime.of(16,0));
            assertThat(calendar.hours(start.toLocalDate().plusDays(1)).close()).isEqualTo(LocalTime.of(18,0));
            jdbc.update("INSERT INTO holidays(date,name,source,open_override,updated_at) VALUES ('2026-10-09','B2-01-test','ADMIN',false,?)",start);
            assertThat(calendars.read(model).isClosed(LocalDate.of(2026,10,9))).isTrue();
            jdbc.update("UPDATE holidays SET open_override=true WHERE date='2026-10-09'");
            em.clear();
            assertThat(calendars.read(model).isClosed(LocalDate.of(2026,10,9))).isFalse();
            assertThat(calendars.read(model).isClosed(LocalDate.of(2026,10,10))).isTrue();
        });
    }
}
