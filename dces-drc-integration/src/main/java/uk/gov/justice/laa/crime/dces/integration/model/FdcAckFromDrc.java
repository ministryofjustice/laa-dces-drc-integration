package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Builder;

import java.util.Map;

@Builder
public record FdcAckFromDrc(FdcAckData data, Map<String, String> meta) {

    @Builder
    public record FdcAckData(Long fdcId, Long maatId, ProcessingReport report) {
    }

}