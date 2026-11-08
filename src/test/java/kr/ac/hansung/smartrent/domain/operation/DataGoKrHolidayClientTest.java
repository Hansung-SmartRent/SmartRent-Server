package kr.ac.hansung.smartrent.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import kr.ac.hansung.smartrent.domain.operation.client.DataGoKrHolidayClient;
import kr.ac.hansung.smartrent.domain.operation.client.HolidayClient.PublicHoliday;
import kr.ac.hansung.smartrent.domain.operation.config.HolidayProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * B1-19 공휴일 받기의 HTTP 호출과 응답 해석. 공공데이터포털 대신 이 시험이 띄운 작은 서버에 붙입니다.
 * 실제 서비스 키로 2027년 공휴일을 받는 확인은 키 발급 뒤 실제 환경에서 따로 합니다.
 */
class DataGoKrHolidayClientTest {

	HttpServer server;
	final AtomicReference<String> lastQuery = new AtomicReference<>();
	volatile String responseBody = "";

	@BeforeEach
	void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", this::handle);
		server.start();
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	private void handle(HttpExchange ex) throws IOException {
		lastQuery.set(ex.getRequestURI().getRawQuery());
		byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
		ex.sendResponseHeaders(200, bytes.length);
		ex.getResponseBody().write(bytes);
		ex.close();
	}

	private DataGoKrHolidayClient client() {
		var properties = new HolidayProperties(true, "test+key/==", "http://127.0.0.1:" + server.getAddress().getPort(),
			Duration.ofSeconds(2));
		return new DataGoKrHolidayClient(properties, JsonMapper.builder().build());
	}

	@Test
	void B1_19_5_여러_건_응답은_공휴일만_날짜와_이름으로() {
		responseBody = """
			{"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
			"body":{"items":{"item":[
			{"dateKind":"01","dateName":"삼일절","isHoliday":"Y","locdate":20270301,"seq":1},
			{"dateKind":"01","dateName":"제헌절","isHoliday":"N","locdate":20270717,"seq":1},
			{"dateKind":"01","dateName":"한글날","isHoliday":"Y","locdate":20271009,"seq":1}]},
			"numOfRows":100,"pageNo":1,"totalCount":3}}}
			""";

		assertThat(client().fetch(2027)).containsExactly(
			new PublicHoliday(LocalDate.of(2027, 3, 1), "삼일절"),
			new PublicHoliday(LocalDate.of(2027, 10, 9), "한글날"));
		assertThat(lastQuery.get()).contains("solYear=2027").contains("_type=json")
			.contains("serviceKey=test%2Bkey%2F%3D%3D");
	}

	@Test
	void B1_19_5_한_건이면_배열이_아니어도_읽고_없으면_빈_목록() {
		responseBody = """
			{"response":{"header":{"resultCode":"00"},"body":{"items":{"item":
			{"dateName":"신정","isHoliday":"Y","locdate":20270101}},"totalCount":1}}}
			""";
		assertThat(client().fetch(2027)).containsExactly(new PublicHoliday(LocalDate.of(2027, 1, 1), "신정"));

		responseBody = """
			{"response":{"header":{"resultCode":"00"},"body":{"items":"","totalCount":0}}}
			""";
		assertThat(client().fetch(2027)).isEmpty();
	}

	@Test
	void B1_19_6_키_오류_XML이나_오류_코드면_예외() {
		responseBody = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>"
			+ "<returnReasonCode>30</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>";
		assertThatThrownBy(() -> client().fetch(2027)).isInstanceOf(IllegalStateException.class);

		responseBody = """
			{"response":{"header":{"resultCode":"30","resultMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR"}}}
			""";
		assertThatThrownBy(() -> client().fetch(2027)).isInstanceOf(IllegalStateException.class)
			.hasMessageNotContaining("test+key");
	}

	@Test
	void 켜져_있는데_키가_비면_서버를_켜지_않고_빠진_이름만_알림() {
		assertThatThrownBy(() -> new HolidayProperties(true, " ", null, null))
			.isInstanceOf(IllegalStateException.class).hasMessageContaining("HOLIDAY_API_KEY");
		assertThat(new HolidayProperties(false, null, null, null).toString()).doesNotContain("apiKey");
	}
}
