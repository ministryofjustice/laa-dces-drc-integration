package uk.gov.justice.laa.crime.dces.integration.configuration.wrapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceData {

    private String correlationId;
    private String traceId;
    private Timestamp timestamp;
}