package kr.ac.hansung.smartrent.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import kr.ac.hansung.smartrent.domain.recommendation.client.GeminiClient;
import kr.ac.hansung.smartrent.domain.recommendation.config.AiProperties;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * B1-21 서버 구현 후 확인할 것 4·7행을 실제 HTTP 호출로 확인합니다. Gemini 대신 이 시험이 띄운 작은 서버에 붙입니다.
 * 키 오류·한도 초과(429)·시간 초과·답 모양 오류는 모두 AI_UNAVAILABLE이고 한 번만 부릅니다.
 */
class GeminiClientTest {

	HttpServer server;
	final AtomicInteger hits = new AtomicInteger();
	final AtomicReference<String> lastPath = new AtomicReference<>();
	final AtomicReference<String> lastKeyHeader = new AtomicReference<>();
	volatile int statusCode = 200;
	volatile String responseBody = "";
	volatile long delayMillis = 0;

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
		hits.incrementAndGet();
		lastPath.set(ex.getRequestURI().toString());
		lastKeyHeader.set(ex.getRequestHeaders().getFirst("x-goog-api-key"));
		ex.getRequestBody().readAllBytes();
		try {
			Thread.sleep(delayMillis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().add("Content-Type", "application/json");
		ex.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);
		if (bytes.length > 0) {
			ex.getResponseBody().write(bytes);
		}
		ex.close();
	}

	private GeminiClient client() {
		AiProperties p = new AiProperties(true, "test-only-key", "test-model", false,
			"http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofMillis(800));
		return new GeminiClient(p, JsonMapper.builder().build());
	}

	private static void assertUnavailable(Runnable call) {
		assertThatThrownBy(call::run).isInstanceOfSatisfying(BusinessException.class,
			e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_UNAVAILABLE));
	}

	@Test
	void B1_21_정상_답은_첫_후보의_글을_돌려주고_키는_머리글로만_보낸다() {
		responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"items\\\":[]}\"}]}}]}";
		assertThat(client().generate("지시문")).isEqualTo("{\"items\":[]}");
		assertThat(lastPath.get()).isEqualTo("/v1beta/models/test-model:generateContent").doesNotContain("test-only-key");
		assertThat(lastKeyHeader.get()).isEqualTo("test-only-key");
	}

	@Test
	void B1_21_7_한도_초과_429는_503이고_다시_부르지_않는다() {
		statusCode = 429;
		responseBody = "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\"}}";
		assertUnavailable(() -> client().generate("지시문"));
		assertThat(hits.get()).isEqualTo(1);
	}

	@Test
	void B1_21_4_키_오류는_503() {
		statusCode = 403;
		responseBody = "{\"error\":{\"status\":\"PERMISSION_DENIED\"}}";
		assertUnavailable(() -> client().generate("지시문"));
		assertThat(hits.get()).isEqualTo(1);
	}

	@Test
	void B1_21_4_시간_초과는_503이고_다시_부르지_않는다() {
		delayMillis = 2000;
		responseBody = "{\"candidates\":[]}";
		assertUnavailable(() -> client().generate("지시문"));
		assertThat(hits.get()).isEqualTo(1);
	}

	@Test
	void B1_21_4_후보가_없는_답은_503() {
		responseBody = "{\"candidates\":[]}";
		assertUnavailable(() -> client().generate("지시문"));
		responseBody = "HTML 오류 페이지";
		assertUnavailable(() -> client().generate("지시문"));
	}

	@Test
	void B1_21_켜져_있는데_키나_모델_ID가_비면_이름만_알리고_멈춘다() {
		assertThatThrownBy(() -> new AiProperties(true, "", "", false, "http://x", Duration.ofSeconds(20)))
			.hasMessageContaining("GEMINI_API_KEY").hasMessageContaining("AI_MODEL_ID");
		assertThatThrownBy(() -> new AiProperties(true, "secret-value", " ", false, "http://x", Duration.ofSeconds(20)))
			.hasMessageContaining("AI_MODEL_ID").hasMessageNotContaining("secret-value");
		assertThat(new AiProperties(true, "secret-value", "m", false, "http://x", null).toString()).doesNotContain("secret-value");
	}
}
