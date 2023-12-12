package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcurContribEntry;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContributionGetResponse {
    List<ConcurContribEntry> files;
}