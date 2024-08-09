package uk.gov.justice.laa.crime.dces.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
public class DcesDrcIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DcesDrcIntegrationApplication.class, args);
	}


}
