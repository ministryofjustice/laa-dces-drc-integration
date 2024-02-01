package uk.gov.justice.laa.crime.dces.integration.configuration.handlers;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = OpenAPI30Configuration.AUTHORIZATION_SCHEMA_NAME,
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenAPI30Configuration {
    public static final String AUTHORIZATION_SCHEMA_NAME = "Authorization";
}