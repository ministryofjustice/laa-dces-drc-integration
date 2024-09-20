package uk.gov.justice.laa.crime.dces.integration.model;

import java.util.Map;

public record FdcAckFromDrc(FdcAckData data, Map<String, String> meta) {
    public record FdcAckData(int fdcId, Integer maatId, String errorText) {
    }

    public static FdcAckFromDrc of(int fdcId, String errorText) {
        return new FdcAckFromDrc(new FdcAckData(fdcId, null, errorText), Map.of());
    }
}
