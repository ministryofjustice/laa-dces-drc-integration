package uk.gov.justice.laa.crime.dces.integration.model.exception.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomDataResponse<T> {
    private T data;
    private TraceData trace;
}