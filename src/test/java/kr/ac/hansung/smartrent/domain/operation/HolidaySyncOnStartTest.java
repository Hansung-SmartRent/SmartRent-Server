package kr.ac.hansung.smartrent.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.ac.hansung.smartrent.domain.operation.service.HolidaySyncScheduler;
import kr.ac.hansung.smartrent.domain.operation.service.HolidaySyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** 운영 서버를 처음 켜면 공휴일이 비어 있던 문제: 공휴일 받기가 켜져 있으면 켜진 직후에도 한 번 받음 */
class HolidaySyncOnStartTest {

	@Configuration
	@Import(HolidaySyncScheduler.class)
	static class Config {
		@Bean
		HolidaySyncService holidaySyncService() {
			return mock(HolidaySyncService.class);
		}
	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

	private void ready(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context, null));
	}

	@Test
	void 공휴일_받기가_켜져_있으면_켜진_직후_한_번_받음() {
		runner.withPropertyValues("smartrent.holiday.sync-enabled=true").run(context -> {
			HolidaySyncService service = context.getBean(HolidaySyncService.class);
			ready(context.getSourceApplicationContext());
			verify(service, times(1)).sync();
		});
	}

	@Test
	void 공휴일_받기가_꺼져_있으면_켜질_때도_받지_않음() {
		runner.withPropertyValues("smartrent.holiday.sync-enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(HolidaySyncScheduler.class);
			HolidaySyncService service = context.getBean(HolidaySyncService.class);
			ready(context.getSourceApplicationContext());
			verify(service, times(0)).sync();
		});
	}

	@Test
	void 켜질_때_받기가_실패해도_예외가_나가지_않음() {
		HolidaySyncService service = mock(HolidaySyncService.class);
		when(service.sync()).thenReturn(-1);

		new HolidaySyncScheduler(service).runOnStart();

		verify(service, times(1)).sync();
	}
}
