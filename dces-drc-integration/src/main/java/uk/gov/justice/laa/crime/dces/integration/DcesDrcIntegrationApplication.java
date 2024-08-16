package uk.gov.justice.laa.crime.dces.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import uk.gov.justice.laa.crime.dces.integration.utils.EnvironmentLoggingListener;


@SpringBootApplication
@ConfigurationPropertiesScan
public class DcesDrcIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DcesDrcIntegrationApplication.class);
		app.addListeners(new EnvironmentLoggingListener());
		app.run(args);
	}



}
