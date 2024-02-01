package uk.gov.justice.laa.crime.dces.integration.configuration.handlers;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static uk.gov.justice.laa.crime.dces.integration.configuration.handlers.OpenApiSwaggerUtils.createExample;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI(
            @Value("${api.title:ApiTitle}") final String title,
            @Value("${api.description:description}") final String description,
            @Value("${api.version:Test}") final String apiVersion,
            @Value("${api.contactName:Test}") final String name,
            @Value("${api.contactEmail:Test}") final String email,
            @Value("${api.contactUrl:Test}") final String url) {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(apiVersion)
                        .contact(new Contact().name(name).email(email).url(url))
                );
    }

    //this group will expose the API which are ready for clients.
    @Bean
    GroupedOpenApi releasedApi() {
        return GroupedOpenApi.builder()
                .displayName("DCES DRC Integration")
                .group("DCES DRC Integration Rest Api v1")
                .pathsToMatch("/api/internal/v1/dces-drc-integration/**")

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

    //add the path to exclude when releasing the code in the prod.
    @Bean
    GroupedOpenApi underDevelopmentApi() {
        return GroupedOpenApi.builder()
                .group("DCES DRC Under Development API")
                .pathsToExclude("/api/internal/v1/dces-drc-integration")
                .build();
    }
}
