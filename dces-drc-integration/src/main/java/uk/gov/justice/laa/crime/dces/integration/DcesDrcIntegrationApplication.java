package uk.gov.justice.laa.crime.dces.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.File;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
public class DcesDrcIntegrationApplication {

	/**
	 * Additional profile for client authentication may be set by this class based on environment variables.
	 * <p>
	 * Temporary measure until everyone is comfortable with mTLS and all tests and developers are upgraded.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		var app = new SpringApplication(DcesDrcIntegrationApplication.class);
		setAdditionalProfiles(app);
		app.run(args);
	}

	/**
	 * Activate the Spring Boot profile named "drc-client-tls" if the environment variables
	 * "DRCCLIENT_KEYSTORE_CERTIFICATE", "DRCCLIENT_KEYSTORE_PRIVATEKEY" and "DRCCLIENT_KEYSTORE_PRIVATEKEYPASSWORD"
	 * are set (and the first two are files).
	 * <p>
	 * This will cause the `SslBundle` named "drc-client" (which is defined in `application-drc-client-tls.yaml`) to be
	 * loaded when the Spring Boot application context loads (during application initialization). Otherwise, the
	 * bundle will not be available to the application, but the application will still succeed to initialize.
	 * <p>
	 * Unfortunately `@SpringBootTest` tests don't use the `SpringApplication`, so for testing you can set the extra
	 * environment variable "SPRING_PROFILES_ACTIVE=drc-client-tls" to make tests use TLS client authentication.
	 * <p>
	 * Temporary measure until everyone is comfortable with mTLS and all tests and developers are upgraded.
	 */
	private static void setAdditionalProfiles(SpringApplication app) {
		String certificate = System.getenv("DRCCLIENT_KEYSTORE_CERTIFICATE");
		String privateKey = System.getenv("DRCCLIENT_KEYSTORE_PRIVATEKEY");
		String privateKeyPassword = System.getenv("DRCCLIENT_KEYSTORE_PRIVATEKEYPASSWORD");

		if (certificate != null && new File(certificate).exists() &&
				privateKey != null && new File(privateKey).exists() &&
				privateKeyPassword != null) {
			app.setAdditionalProfiles("drc-client-tls");
		}
	}
}
