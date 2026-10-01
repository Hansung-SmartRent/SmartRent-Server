package kr.ac.hansung.smartrent.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** B1-01 서버 구현 후 확인할 것 6행: ErrorCode와 API 명세 "오류 코드 전체" 표 비교 */
class ErrorCodeTest {

	private static final Pattern ROW = Pattern.compile("^\\| `([A-Z_]+)` \\| (\\d{3}) \\| ([^|]+?) \\|", Pattern.MULTILINE);

	@Test
	void B1_01_6_ErrorCode_52개가_명세_표와_이름_상태_메시지가_같다() throws IOException {
		Map<String, String> spec = readSpecTable();
		Map<String, String> code = new LinkedHashMap<>();
		Arrays.stream(ErrorCode.values())
			.forEach(e -> code.put(e.name(), e.getStatus().value() + " " + e.getMessage()));

		assertThat(spec).hasSize(52);
		assertThat(code).containsExactlyEntriesOf(spec);
	}

	private Map<String, String> readSpecTable() throws IOException {
		String doc = Files.readString(Path.of("docs/api/README.md"), StandardCharsets.UTF_8);
		String section = doc.substring(doc.indexOf("#### 오류 코드 전체"));
		section = section.substring(0, section.indexOf("\n### "));
		Map<String, String> rows = new LinkedHashMap<>();
		Matcher m = ROW.matcher(section);
		while (m.find()) {
			rows.put(m.group(1), m.group(2) + " " + m.group(3).strip());
		}
		return rows;
	}
}
