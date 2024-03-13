package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class UpdateRequest {

    private int recordsSent;
    @NonNull
    private String xmlContent;
    @NonNull
    private String xmlFileName;
    @NonNull
    private String ackXmlContent;

}
