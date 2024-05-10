package uk.gov.justice.laa.crime.dces.integration.model.exception;

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

    private String message;
    private Integer statusCode;
    private String traceId;
    private String correlationId;
    @Builder.Default
    private List<ErrorSubMessage> subMessages = new ArrayList<>();
}