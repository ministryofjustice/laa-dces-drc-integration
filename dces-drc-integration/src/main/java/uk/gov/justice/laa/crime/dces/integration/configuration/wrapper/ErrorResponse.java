package uk.gov.justice.laa.crime.dces.integration.configuration.wrapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.crime.dces.integration.model.exception.TraceData;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse<T> {
    private T data;
    private TraceData trace;
}