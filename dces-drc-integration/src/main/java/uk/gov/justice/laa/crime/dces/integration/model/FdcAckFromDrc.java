package uk.gov.justice.laa.crime.dces.integration.model;

import java.util.Map;

public record FdcAckFromDrc(Data data, Map<String, String> meta) {
    public record Data(int id, Integer maatId, String errorText) {
    }

    public static FdcAckFromDrc of(int fdcId, String errorText) {
        return new FdcAckFromDrc(new Data(fdcId, null, errorText), Map.of());
    }
}
