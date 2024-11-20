package uk.gov.justice.laa.crime.dces.integration.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
@Data
public class ServicesProperties {
    @NotNull
    private MaatApi maatApi;

    @NotNull
    private DrcClientApi drcClientApi;

    @Data
    public static class MaatApi {
        @NotNull
        private String baseUrl;

        private boolean oAuthEnabled;

        private int maxBufferSize = 1;

        private int getContributionBatchSize;
    }

    @Data
    public static class DrcClientApi {
        @NotNull
        private String baseUrl;

        private boolean oAuthEnabled;
    }
}
