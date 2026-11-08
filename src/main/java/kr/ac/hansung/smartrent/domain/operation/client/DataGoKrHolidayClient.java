package kr.ac.hansung.smartrent.domain.operation.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import kr.ac.hansung.smartrent.domain.operation.config.HolidayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 공공데이터포털 한국천문연구원 특일 정보의 공휴일 정보 조회(getRestDeInfo, 외부 연결 4절).
 * 서비스 키는 포털의 일반 인증키(Decoding)를 넣고, 요청할 때 주소용으로 바꿔 보냅니다. 키와 주소는 로그에 남기지 않습니다.
 * 응답이 JSON이 아니거나 resultCode가 00이 아니면(키 오류 등) 실패로 보고 예외를 던집니다.
 */
@Slf4j
public class DataGoKrHolidayClient implements HolidayClient {

	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient restClient;
	private final HolidayProperties properties;
	private final JsonMapper jsonMapper;

	public DataGoKrHolidayClient(HolidayProperties properties, JsonMapper jsonMapper) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		var http = java.net.http.HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
		var factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(properties.timeout());
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

	@Override
	public List<PublicHoliday> fetch(int year) {
		URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl() + "/getRestDeInfo")
			.queryParam("serviceKey", "{key}")
			.queryParam("solYear", year)
			.queryParam("numOfRows", 100)
			.queryParam("_type", "json")
			.encode()
			.buildAndExpand(properties.apiKey())
			.toUri();
		String raw;
		try {
			// 글자 집합을 알리지 않는 응답도 있어 바이트로 받아 UTF-8로 읽음
			byte[] bytes = restClient.get().uri(uri).retrieve().body(byte[].class);
			raw = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
		}
		catch (RuntimeException e) {
			throw new IllegalStateException("공휴일 받기 실패: " + e.getClass().getSimpleName());
		}
		return parse(raw);
	}

	List<PublicHoliday> parse(String raw) {
		JsonNode root;
		try {
			root = jsonMapper.readTree(raw == null ? "" : raw);
		}
		catch (RuntimeException e) {
			throw new IllegalStateException("공휴일 응답이 JSON이 아닙니다(키 오류일 수 있음)");
		}
		JsonNode response = root.path("response");
		String code = response.path("header").path("resultCode").asString("");
		if (!"00".equals(code)) {
			throw new IllegalStateException("공휴일 응답 오류 코드: " + code);
		}
		JsonNode item = response.path("body").path("items").path("item");
		List<PublicHoliday> result = new ArrayList<>();
		if (item.isArray()) {
			item.forEach(node -> add(result, node));
		}
		else if (item.isObject()) {
			add(result, item);
		}
		return result;
	}

	private static void add(List<PublicHoliday> result, JsonNode node) {
		if (!"Y".equals(node.path("isHoliday").asString(""))) {
			return;
		}
		String date = node.path("locdate").asString("");
		String name = node.path("dateName").asString("").trim();
		result.add(new PublicHoliday(LocalDate.parse(date, BASIC), name.length() > 50 ? name.substring(0, 50) : name));
	}
}
