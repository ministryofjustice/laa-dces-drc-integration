package uk.gov.justice.laa.crime.dces.integration.rest.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorSubMessage {

    private String errorCode;
    private String field;
    private String value;
    private String message;
}