package kr.ac.hansung.smartrent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SmartrentApplicationTests {

	@Test
	void contextLoads() {
	}

}
