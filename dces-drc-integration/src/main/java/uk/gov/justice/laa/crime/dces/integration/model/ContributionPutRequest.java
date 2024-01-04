package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ContributionPutRequest {

    private int recordsSent;
    private String xmlContent;
    private List<String> concorContributionIds;
    private String xmlFileName;
    private String ackXmlContent;

}
