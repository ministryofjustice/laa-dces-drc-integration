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
	 * Activate the Spring Boot profile named "client-auth" if the environment variables "CLIENT_AUTH_CERTIFICATE",
	 * "CLIENT_AUTH_PRIVATE_KEY" and "CLIENT_AUTH_PRIVATE_KEY_PASSWORD" are set (and first two are files).
	 * <p>
	 * This will cause the `SslBundle` named "client-auth" (which is defined in `application-client-auth.yaml`) to be
	 * loaded when the Spring Boot application context loads (during application initialization). Otherwise, the
	 * bundle will not be available to the application, but the application will still succeed to initialize.
	 * <p>
	 * Unfortunately `@SpringBootTest` tests don't use the `SpringApplication`, so for testing you can set the extra
	 * environment variable "SPRING_PROFILES_ACTIVE=client-auth" to make tests use TLS client authentication.
	 * <p>
	 * Temporary measure until everyone is comfortable with mTLS and all tests and developers are upgraded.
	 */
	private static void setAdditionalProfiles(SpringApplication app) {
		String clientAuthCertificate = System.getenv("CLIENT_AUTH_CERTIFICATE");
		String clientAuthPrivateKey = System.getenv("CLIENT_AUTH_PRIVATE_KEY");
		String clientAuthPrivateKeyPassword = System.getenv("CLIENT_AUTH_PRIVATE_KEY_PASSWORD");

		if (clientAuthCertificate != null && new File(clientAuthCertificate).exists() &&
				clientAuthPrivateKey != null && new File(clientAuthPrivateKey).exists() &&
				clientAuthPrivateKeyPassword != null) {
			app.setAdditionalProfiles("client-auth");
		}
	}
}
