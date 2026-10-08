package kr.ac.hansung.smartrent;

import java.util.TimeZone;

import kr.ac.hansung.smartrent.global.config.RequiredSettingsCheck;
import kr.ac.hansung.smartrent.global.config.TimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartrentApplication {

	static {
		// 시험에서도 적용되도록 클래스를 읽을 때 JVM 기본 시간대를 한국 시각으로 둠
		TimeZone.setDefault(TimeZone.getTimeZone(TimeConfig.ZONE));
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SmartrentApplication.class);
		app.addListeners(new RequiredSettingsCheck());
		app.run(args);
	}

}
