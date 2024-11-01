package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EventType {

    FETCHED_FROM_MAAT("FetchedFromMAAT"),
    SENT_TO_DRC("SyncRequestResponseToDrc"),
    FDC_GLOBAL_UPDATE("FdcGlobalUpdate"),
    UPDATED_IN_MAAT("SyncResponseLoggedToMAAT"),
    DRC_ASYNC_RESPONSE("AsyncRequestResponseFromDrc");
    private final String name;

    @Override
    public String toString() {
        return this.getName();
    }
}
