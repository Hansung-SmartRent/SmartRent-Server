package kr.ac.hansung.smartrent.global.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** 서버가 켜질 때 처음 데이터를 넣습니다. local은 항상, prod는 SEED_PROD_ENABLED=true일 때만. 시험(기본 프로필)에서는 넣지 않습니다. */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SeedProperties.class)
public class SeedRunner implements ApplicationRunner {

	private final SeedService seedService;
	private final SeedProperties properties;
	private final Environment environment;

	@Override
	public void run(ApplicationArguments args) {
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			seedService.loadLocal();
		}
		else if (environment.acceptsProfiles(Profiles.of("prod")) && properties.prodEnabled()) {
			seedService.loadProd(properties.adminEmail(), properties.adminPassword());
		}
	}
}
