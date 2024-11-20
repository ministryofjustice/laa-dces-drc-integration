package uk.gov.justice.laa.crime.dces.integration.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static uk.gov.justice.laa.crime.dces.integration.utils.OpenApiSwaggerUtils.createExample;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI(
            @Value("${api.title:ApiTitle}") final String title,
            @Value("${api.description:description}") final String description,
            @Value("${api.version:Test}") final String apiVersion,
            @Value("${api.contactName:Test}") final String name,
            @Value("${api.contactEmail:Test}") final String email,
            @Value("${api.contactUrl:Test}") final String url,
            @Value("${spring.security.oauth2.client.provider.drc-client-api.token-uri:TestURI}") final String oAuthTokenUri) {
        return new OpenAPI()
                .components(new Components()
                    .addSecuritySchemes("OAuth2",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(new OAuthFlows()
                                .clientCredentials(new OAuthFlow()
                                    .tokenUrl(oAuthTokenUri)
                                )
                            )
                    )
                )
                .addSecurityItem(new SecurityRequirement().addList("OAuth2"))
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(apiVersion)
                        .contact(new Contact().name(name).email(email).url(url))
                );
    }

   @Bean
    GroupedOpenApi releasedApi() {
        return groupedOpenApiBuilderBase()
                .displayName("DCES DRC Integration API v1")
                .group("1 DCES DRC Integration Rest Api v1") //This determines the order in which the groups are displayed
                .pathsToMatch("/api/dces/v1/**")
                .addOperationCustomizer((operation, method) -> {
                    operation.addParametersItem(
                            new HeaderParameter()
                                    .name("CLIENT_HEADER")
                                    .description("Rest API Client's identifier.")
                                    .addExample("DCES", createExample("DCES"))
                                    .addExample("MARSTON", createExample("MARSTON"))
                                    .addExample("INTERNAL", createExample("INTERNAL"))
                                    .required(false)
                    );
                    return operation;
                })
                .build();
    }

    @Bean
    GroupedOpenApi underDevelopmentApi() {
        return groupedOpenApiBuilderBase()
            .displayName("DCES DRC Integration API Under Development")
            .group("3 DCES DRC API Under Development") //This determines the order in which the groups are displayed
            .pathsToMatch("/api/dces/dev/**")
            .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                .title("DCES DRC API Endpoints Under Development")
                .description("Endpoints that are under development (if any), will be documented here. "  )))
            .build();
    }

  @Bean
  GroupedOpenApi testApi() {
    return groupedOpenApiBuilderBase()
        .displayName("DCES DRC Integration API Test and Stub endpoints")
        .group("2 DCES DRC API Test and Stub endpoints") //This determines the order in which the groups are displayed
        .pathsToMatch("/api/dces/test/**", "/api/dces/v1-stub/**")
        .addOpenApiCustomizer(openApi -> openApi.info(new Info()
            .title("DCES DRC API Test and Stub endpoints")
            .description("If the endpoints meant for testing and stubbing are enabled in this environment, they will be documented here. "  )))
        .build();
  }

  private GroupedOpenApi.Builder groupedOpenApiBuilderBase() {
    return GroupedOpenApi.builder()
        .pathsToExclude("/error");
  }

}
