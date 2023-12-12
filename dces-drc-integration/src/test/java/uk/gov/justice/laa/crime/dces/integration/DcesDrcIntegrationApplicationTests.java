package uk.gov.justice.laa.crime.dces.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@EnableAutoConfiguration
@ActiveProfiles("test")
class DcesDrcIntegrationApplicationTests {

	@java.lang.SuppressWarnings("squid:S2699")
	@Test
	void contextLoads() {
	}

}
