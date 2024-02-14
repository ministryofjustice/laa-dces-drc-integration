package uk.gov.justice.laa.crime.dces.integration.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorSubMessage {

    @JsonProperty("errorCode")
    private String errorCode;
    @JsonProperty("field")
    private String field;
    @JsonProperty("value")
    private String value;
    @JsonProperty("message")
    private String message;

}