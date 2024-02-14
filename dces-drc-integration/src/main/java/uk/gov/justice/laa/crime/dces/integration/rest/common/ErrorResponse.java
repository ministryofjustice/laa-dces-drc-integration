package uk.gov.justice.laa.crime.dces.integration.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("correlationId")
    private String correlationId;

    @Builder.Default
    @JsonProperty("subMessages")
    private List<ErrorSubMessage> subMessages = new ArrayList<>();
}
