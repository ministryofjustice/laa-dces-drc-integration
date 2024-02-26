package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@AllArgsConstructor
public class ContributionPutRequest {

    private int recordsSent;
    @NonNull
    private String xmlContent;
    @NonNull
    private List<String> concorContributionIds;
    @NonNull
    private String xmlFileName;
    @NonNull
    private String ackXmlContent;

}
