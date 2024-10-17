package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RecordType {

    CONTRIBUTION("Contribution"), FDC("Fdc");
    private final String name;

}
