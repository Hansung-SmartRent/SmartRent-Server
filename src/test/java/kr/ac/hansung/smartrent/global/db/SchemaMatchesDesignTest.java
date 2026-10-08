package kr.ac.hansung.smartrent.global.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.ac.hansung.smartrent.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * B1-02 서버 구현 후 확인할 것 1·2행과 완료 조건 2:
 * Flyway가 만든 표의 칸 이름·자료형·NOT NULL·유일 조건이 docs/db/README.md 3절과 같은지 문서를 직접 읽어 비교한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaMatchesDesignTest {

	/** 칸별 자료형이 문서에 적힌 백엔드 1 표 */
	static final List<String> TYPED_TABLES = List.of("users", "email_verifications", "login_failures", "refresh_tokens",
		"equipment_models", "model_images", "equipment_units", "operating_hours", "holidays");
	/** 문서에 칸 이름만 적힌 표(notices, inquiries, purchase_requests, recommendation_logs, audit_logs) */
	static final List<String> NAME_ONLY_TABLES = List.of("notices", "inquiries", "purchase_requests", "recommendation_logs", "audit_logs");

	@Autowired
	JdbcTemplate jdbc;

	record ColumnSpec(String type, boolean notNull, boolean unique, boolean primaryKey) {
	}

	@Test
	void B1_02_1_백엔드1_표가_모두_생성된다() {
		List<String> tables = jdbc.queryForList(
			"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
		assertThat(tables).containsAll(TYPED_TABLES).containsAll(NAME_ONLY_TABLES);
	}

	@Test
	void B1_02_2_칸의_이름_자료형_NOT_NULL_유일이_DB설계와_같다() throws IOException {
		Map<String, Map<String, ColumnSpec>> design = readDesign();
		for (String table : TYPED_TABLES) {
			Map<String, ColumnSpec> expected = design.get(table);
			assertThat(expected).as("문서에서 %s 표를 찾지 못함", table).isNotNull();
			assertThat(actual(table)).as(table).containsExactlyInAnyOrderEntriesOf(expected);
		}
	}

	@Test
	void B1_02_2_칸_이름만_적힌_표는_칸_이름이_같다() throws IOException {
		Map<String, Set<String>> names = readNameOnlyTables();
		for (String table : NAME_ONLY_TABLES) {
			assertThat(actual(table).keySet()).as(table).containsExactlyInAnyOrderElementsOf(names.get(table));
		}
	}

	@Test
	void B1_02_2_users는_email과_student_number가_유일하고_시각은_DATETIME() {
		String ddl = jdbc.queryForMap("SHOW CREATE TABLE users").get("Create Table").toString();
		assertThat(ddl).contains("UNIQUE KEY `uk_users_email` (`email`)")
			.contains("UNIQUE KEY `uk_users_student_number` (`student_number`)")
			.contains("`approval` varchar(10) NOT NULL")
			.contains("`created_at` datetime NOT NULL");
	}

	private Map<String, ColumnSpec> actual(String table) {
		Set<String> unique = new TreeSet<>(jdbc.queryForList("""
			SELECT column_name FROM information_schema.statistics s
			WHERE table_schema = DATABASE() AND table_name = ? AND non_unique = 0 AND index_name <> 'PRIMARY'
			  AND (SELECT COUNT(*) FROM information_schema.statistics s2 WHERE s2.table_schema = s.table_schema
			       AND s2.table_name = s.table_name AND s2.index_name = s.index_name) = 1
			""", String.class, table));
		Map<String, ColumnSpec> result = new LinkedHashMap<>();
		jdbc.query("""
			SELECT column_name, column_type, is_nullable, column_key FROM information_schema.columns
			WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position
			""", rs -> {
			String name = rs.getString(1);
			result.put(name, new ColumnSpec(rs.getString(2).toLowerCase(Locale.ROOT), "NO".equals(rs.getString(3)),
				unique.contains(name), "PRI".equals(rs.getString(4))));
		}, table);
		return result;
	}

	/** 3절의 "### 표 이름" 아래 | 칸 | 자료형 | 필수 | 뜻 | 표를 읽는다 */
	private Map<String, Map<String, ColumnSpec>> readDesign() throws IOException {
		String doc = Files.readString(Path.of("docs/db/README.md"), StandardCharsets.UTF_8);
		Map<String, Map<String, ColumnSpec>> result = new LinkedHashMap<>();
		Matcher heading = Pattern.compile("^### ([a-z_]+)\\b.*$", Pattern.MULTILINE).matcher(doc);
		List<int[]> spans = new ArrayList<>();
		List<String> names = new ArrayList<>();
		while (heading.find()) {
			names.add(heading.group(1));
			spans.add(new int[] {heading.end(), 0});
		}
		for (int i = 0; i < names.size(); i++) {
			int end = i + 1 < spans.size() ? doc.lastIndexOf("###", spans.get(i + 1)[0]) : doc.length();
			String section = doc.substring(spans.get(i)[0], end);
			Map<String, ColumnSpec> cols = new LinkedHashMap<>();
			for (String line : section.split("\n")) {
				String[] cells = line.split("\\|");
				if (cells.length < 4 || !cells[2].trim().matches("[A-Z]+(\\(\\d+\\))?")) {
					continue;
				}
				String required = cells[3].trim();
				ColumnSpec spec = new ColumnSpec(sqlType(cells[2].trim()), required.startsWith("필수"),
					required.contains("유일"), required.contains("기본 키") || cells[1].trim().equals("id"));
				for (String col : cells[1].split(",")) {
					String c = col.trim();
					boolean pk = spec.primaryKey() && (c.equals("id") || required.contains("기본 키"));
					cols.put(c, new ColumnSpec(spec.type(), spec.notNull() || pk, spec.unique(), pk));
				}
			}
			if (!cols.isEmpty()) {
				result.put(names.get(i), cols);
			}
		}
		return result;
	}

	private Map<String, Set<String>> readNameOnlyTables() throws IOException {
		String doc = Files.readString(Path.of("docs/db/README.md"), StandardCharsets.UTF_8);
		Map<String, Set<String>> result = new LinkedHashMap<>();
		Matcher row = Pattern.compile("^\\| ([a-z_]+) \\| ([^|]+) \\|", Pattern.MULTILINE).matcher(doc);
		while (row.find()) {
			if (!NAME_ONLY_TABLES.contains(row.group(1))) {
				continue;
			}
			String cols = row.group(2).trim();
			if (cols.startsWith("inquiries와 같은 칸")) {
				cols = result.get("inquiries").toString();
			}
			Set<String> set = new TreeSet<>();
			for (String c : cols.replaceAll("[\\[\\]]", "").split(",")) {
				set.add(c.replaceAll("\\(.*", "").trim());
			}
			result.put(row.group(1), set);
		}
		return result;
	}

	private static String sqlType(String doc) {
		return switch (doc) {
			case "BOOLEAN" -> "tinyint(1)";
			default -> doc.toLowerCase(Locale.ROOT);
		};
	}
}
